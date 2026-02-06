import { Cookie } from 'tough-cookie';
import { LoginResult, ResponseResult } from '../@types/shared';
import { logger } from './logger';
import type { NetworkSessionManager } from './network-session-manager';
import type { ServerConfigService } from './server-config-service';
import type { SessionHeartbeatService } from './session-heartbeat-service';
import store, { ServerConfig, ensureCryptoKeys } from './store';
import { safeRequest } from './utils';

type LoginResponseData = {
  hasDefaultPassword: boolean;
};

const DEFAULT_REFRESH_MS = 15 * 60 * 1000;
const SAFETY_BUFFER_MS = 60 * 1000;

export class AuthService {
  constructor(
    private configService: ServerConfigService,
    private networkManager: NetworkSessionManager,
    private heartbeatService: SessionHeartbeatService,
    private onSessionExpired: (serverId: string) => void
  ) {}

  public async initializeSession(serverId: string): Promise<boolean> {
    const server = this.configService.getServerById(serverId);
    if (!server || !server.sessionCookie) {
      return false;
    }
    logger.info(`[${server.name}] verifying existing session on startup...`);
    const refreshed = await this.refreshSession(serverId);

    if (refreshed) {
      await this.startHeartbeatForServer(serverId);
      return true;
    } else {
      logger.warn(`[${server.name}] session expired or invalid on startup`);
      await this.disconnect(serverId);
      return false;
    }
  }

  public async connect(serverId: string): Promise<LoginResult> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return { success: false, error: 'Server not found' };
    }

    const keys = ensureCryptoKeys();
    const clientId = store.get('clientId');
    const password = this.configService.getDecryptedPassword(server);

    const cleanPubKey = keys.publicKey
      .replace(/-----BEGIN PUBLIC KEY-----/, '')
      .replace(/-----END PUBLIC KEY-----/, '')
      .replace(/\s+/g, '');

    const params = new URLSearchParams();
    params.append('username', server.username);
    params.append('password', password);
    params.append('clientId', clientId);
    params.append('pubKey', cleanPubKey);

    const client = this.networkManager.getAxiosForServer(serverId);
    this.heartbeatService.stop(server);

    const result = await safeRequest<LoginResponseData>(
      () =>
        client.post<LoginResponseData>('/api/login', params, {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        }),
      server.name,
      'login'
    );
    if (!result.success) {
      return result;
    }
    const data = result.data!;
    const hasDefaultPassword = data.hasDefaultPassword;

    const sessionCookieFound = await this.safeUpdateSessionCookie(server, hasDefaultPassword);
    if (sessionCookieFound) {
      logger.info(`[${server.name}] login successfully.`);
      await this.startHeartbeatForServer(serverId);
    }
    return {
      success: sessionCookieFound,
      hasDefaultPassword,
      error: !sessionCookieFound ? 'Login error' : undefined,
    };
  }

  public async disconnect(serverId: string): Promise<boolean> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return true;
    }
    this.heartbeatService.stop(server);
    const client = this.networkManager.getAxiosForServer(serverId);
    const result = await safeRequest<void>(
      () => client.delete('/api/logout'),
      server.name,
      'logout'
    );

    if (result.success) {
      logger.info(`[${server.name}] remote logout successful`);
    } else {
      logger.warn(`[${server.name}] remote logout failed, cleaning locally`);
    }
    this.networkManager.clearSession(serverId);
    this.configService.updateServerSessionData(serverId, undefined, undefined);

    logger.info(`[${server.name}] logged out locally.`);
    return true;
  }

  public async refreshSession(serverId: string): Promise<boolean> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return false;
    }
    const client = this.networkManager.getAxiosForServer(serverId);
    const result = await safeRequest<void>(
      () => client.post('/api/session/refresh'),
      server.name,
      'refresh'
    );
    if (result.success) {
      logger.info(`[${server.name}] session refreshed successfully`);
      await this.safeUpdateSessionCookie(server);
      return true;
    }
    return false;
  }

  public async removeServer(serverId: string): Promise<ResponseResult> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return { success: false, error: 'Server not found' };
    }
    await this.disconnect(serverId);
    this.configService.removeServer(serverId);
    logger.info(`[${server.name}] disconnected and removed from store`);
    return { success: true };
  }

  public async updateDefaultPassword(
    serverId: string,
    newPassword: string
  ): Promise<ResponseResult> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return { success: false, error: 'Server not found' };
    }
    const params = new URLSearchParams();
    params.append('password', newPassword);

    const client = this.networkManager.getAxiosForServer(serverId);
    const result = await safeRequest<void>(
      () =>
        client.post('/api/update/default/password', params, {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        }),
      server.name,
      'updateDefaultPassword'
    );
    if (result.success) {
      logger.info(`[${server.name}] successfully updated password`);
      this.configService.updateServerPassword(serverId, newPassword);
    }
    return result;
  }

  public getActiveSessionIds(): string[] {
    return this.heartbeatService.getActiveIds();
  }

  private async getSessionCookie(server: ServerConfig): Promise<Cookie | undefined> {
    const jar = this.networkManager.getJarForServer(server.id);
    const cookies = await jar.getCookies(server.url);
    return cookies.find(c => c.key === 'sid');
  }

  private async safeUpdateSessionCookie(
    server: ServerConfig,
    hasDefaultPassword?: boolean
  ): Promise<boolean> {
    const sessionCookie = await this.getSessionCookie(server);
    if (sessionCookie) {
      this.configService.updateServerSessionData(
        server.id,
        JSON.stringify(sessionCookie.toJSON()),
        hasDefaultPassword
      );
    }
    return !!sessionCookie;
  }

  private async startHeartbeatForServer(serverId: string): Promise<void> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return;
    }
    const intervalMs = await this.calculateSmartRefreshInterval(server);
    this.heartbeatService.start(
      server,
      async () => {
        const success = await this.refreshSession(serverId);
        if (!success) {
          return false;
        }
        return await this.calculateSmartRefreshInterval(server);
      },
      async () => {
        logger.warn(`[${serverId}] auto-logout due to session expiration`);
        await this.disconnect(serverId);
        this.onSessionExpired(serverId);
      },
      intervalMs
    );
  }

  private async calculateSmartRefreshInterval(server: ServerConfig): Promise<number> {
    const sessionCookie = await this.getSessionCookie(server);
    if (!sessionCookie) {
      return DEFAULT_REFRESH_MS;
    }
    const expires = sessionCookie.expires;
    if (!expires || expires === 'Infinity') {
      return DEFAULT_REFRESH_MS;
    }
    const now = Date.now();
    const expiresMs = new Date(expires).getTime();
    const timeToLive = expiresMs - now;
    if (timeToLive <= 0) {
      return 1000;
    }
    let refreshInterval = timeToLive - SAFETY_BUFFER_MS;
    if (refreshInterval <= 0) {
      refreshInterval = timeToLive * 0.8;
    }
    return refreshInterval;
  }
}
