import type { Cookie } from 'tough-cookie';
import type { ApiResult, LoginData, ResponseResult } from '../../@types/shared';
import { createScopedLogger } from '../logger';
import type { NetworkSessionManager } from '../network/network-session-manager';
import { safeRequest } from '../network/request';
import type { SessionHeartbeatService } from '../network/session-heartbeat-service';
import type { ServerConfig } from '../store';
import store, { ensureCryptoKeys } from '../store';
import type { ConfigService } from './config-service';

type LoginResponseData = {
  hasDefaultPassword: boolean;
};

type Handlers = {
  onHeartbeat: (serverId: string, status: boolean, resTimeMillis?: number) => void;
  onSessionExpired: (serverId: string) => void;
  onConnect: (server: ServerConfig) => Promise<void>;
  onDisconnect: (server: ServerConfig) => Promise<void>;
  onSetupPoint: (serverName: string | undefined, msg: string) => Promise<void>;
};

const DEFAULT_REFRESH_MS = 15 * 60 * 1000;
const SAFETY_BUFFER_MS = 60 * 1000;

export class AuthService {
  private logger = createScopedLogger(this.constructor.name);

  constructor(
    private configService: ConfigService,
    private networkManager: NetworkSessionManager,
    private heartbeatService: SessionHeartbeatService,
    private handlers: Handlers
  ) {}

  public async autoLogin(): Promise<void> {
    const servers = this.configService.getServers();
    if (servers.length === 0) {
      return;
    }
    this.logger.info('?', `checking sessions for ${servers.length} servers...`);
    await this.handlers.onSetupPoint(undefined, 'checking saved sessions');
    await Promise.allSettled(
      servers.map(async server => {
        const isValid = await this.initializeSession(server.id);
        const message = isValid
          ? 'session active, heartbeat started'
          : 'session expired or invalid';
        this.logger.info(server.name, message);
        await this.handlers.onSetupPoint(server.name, message);
      })
    );
  }

  public async initializeSession(serverId: string): Promise<boolean> {
    const server = this.configService.getServerById(serverId);
    if (!server || !server.sessionCookie) {
      return false;
    }
    this.logger.info(server.name, 'verifying existing session on startup...');
    await this.handlers.onSetupPoint(server.name, 'verifying existing session');
    const { success } = await this.refreshSession(serverId);
    if (success) {
      await this.startHeartbeatForServer(server);
      await this.handlers.onConnect(server);
      await this.handlers.onSetupPoint(server.name, 'connected');
      return true;
    } else {
      this.logger.warn(server.name, 'session expired or invalid on startup');
      await this.disconnect(serverId);
      return false;
    }
  }

  public async connect(serverId: string): Promise<ApiResult<LoginData>> {
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

    const client = this.networkManager.getAxiosForServer(server);
    this.heartbeatService.stop(server);

    const url = '/api/login';
    const result = await safeRequest<LoginResponseData>(
      () =>
        client.post<LoginResponseData>(url, params, {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        }),
      server.name,
      url
    );
    if (!result.success) {
      return result;
    }
    const data = result.data!;
    const { hasDefaultPassword, ...rest } = data;

    const sessionCookieFound = await this.safeUpdateSessionCookie(server, hasDefaultPassword);
    if (sessionCookieFound) {
      this.logger.info(server.name, 'login successfully.');
      await this.startHeartbeatForServer(server);
      await this.handlers.onConnect(server);
    }
    return {
      success: sessionCookieFound,
      data: sessionCookieFound
        ? {
            hasDefaultPassword,
            ...rest,
          }
        : undefined,
      error: !sessionCookieFound ? 'Login error' : undefined,
    };
  }

  public async disconnect(serverId: string): Promise<boolean> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return true;
    }
    this.heartbeatService.stop(server);
    await this.handlers.onDisconnect(server);
    const client = this.networkManager.getAxiosForServer(server);
    const url = '/api/logout';
    const result = await safeRequest<void>(() => client.delete(url), server.name, url);

    if (result.success) {
      this.logger.info(server.name, 'remote logout successful');
    } else {
      this.logger.warn(server.name, 'remote logout failed, cleaning locally');
    }
    this.networkManager.clearSession(serverId);
    this.configService.updateServerSessionData(serverId, undefined, undefined);

    this.logger.info(server.name, 'logged out locally');
    return true;
  }

  public async refreshSession(serverId: string): Promise<ResponseResult> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return { success: false };
    }
    const client = this.networkManager.getAxiosForServer(server);
    const url = '/api/session/refresh';
    const { success, resTimeMillis } = await safeRequest<void>(
      () => client.post(url),
      server.name,
      url
    );
    if (success) {
      this.logger.info(server.name, 'session refreshed successfully');
      await this.safeUpdateSessionCookie(server);
    }
    return { success, resTimeMillis };
  }

  public async removeServer(serverId: string): Promise<ResponseResult> {
    const server = this.configService.getServerById(serverId);
    if (!server) {
      return { success: false, error: 'Server not found' };
    }
    await this.disconnect(serverId);
    this.configService.removeServer(serverId);
    this.logger.info(server.name, `disconnected and removed from store`);
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

    const client = this.networkManager.getAxiosForServer(server);
    const url = '/api/update/default/password';
    const result = await safeRequest<void>(
      () =>
        client.post(url, params, {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        }),
      server.name,
      url
    );
    if (result.success) {
      this.logger.info(server.name, 'successfully updated password');
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

  private async startHeartbeatForServer(server: ServerConfig): Promise<void> {
    this.heartbeatService.start(
      server,
      async () => {
        const { success, resTimeMillis } = await this.refreshSession(server.id);
        this.handlers.onHeartbeat(server.id, success, resTimeMillis);
        if (!success) {
          return false;
        }
        return await this.calculateSmartRefreshInterval(server);
      },
      async () => {
        this.logger.warn(server.name, 'auto-logout due to session expiration');
        await this.disconnect(server.id);
        this.handlers.onSessionExpired(server.id);
      }
    );
  }

  private async calculateSmartRefreshInterval(
    server: ServerConfig,
    multiplier: number = 0.1
  ): Promise<number> {
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
    const finalInterval = refreshInterval * multiplier;
    return Math.max(2000, Math.floor(finalInterval));
  }
}
