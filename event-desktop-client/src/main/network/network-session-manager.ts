import type { AxiosInstance } from 'axios';
import axios from 'axios';
import { wrapper } from 'axios-cookiejar-support';
import { Cookie, CookieJar } from 'tough-cookie';
import { createScopedLogger } from '../logger';
import type { ConfigService } from '../service/config-service';
import type { ServerConfig } from '../store';
import { extractErrorMessage } from '../utils';

export class NetworkSessionManager {
  private logger = createScopedLogger(this.constructor.name);
  private cookieJars: Map<string, CookieJar> = new Map();

  constructor(private configService: ConfigService) {}

  public getAxiosForServer({ id, url }: ServerConfig): AxiosInstance {
    return wrapper(
      axios.create({
        baseURL: url,
        jar: this.getJarForServer(id),
        withCredentials: true,
      })
    );
  }

  public getJarForServer(serverId: string): CookieJar {
    if (this.cookieJars.has(serverId)) {
      return this.cookieJars.get(serverId)!;
    }
    const jar = new CookieJar(null, {
      allowSpecialUseDomain: true,
      rejectPublicSuffixes: false,
    });
    const server = this.configService.getServerById(serverId);
    if (server) {
      this.tryRestoreSession(jar, server);
    }
    this.cookieJars.set(serverId, jar);
    return jar;
  }

  public clearSession(serverId: string): void {
    if (this.cookieJars.has(serverId)) {
      this.cookieJars.get(serverId)?.removeAllCookiesSync();
      this.cookieJars.delete(serverId);
    }
  }

  private tryRestoreSession(jar: CookieJar, server: ServerConfig): void {
    if (!server.sessionCookie) {
      return;
    }
    try {
      const cookieObj = JSON.parse(server.sessionCookie) as object;
      const restoredCookie = Cookie.fromJSON(cookieObj);
      if (restoredCookie) {
        jar.setCookieSync(restoredCookie, server.url);
        this.logger.info(server.name, 'restored session from store');
      }
    } catch (err) {
      const errMessage = extractErrorMessage(err);
      this.logger.error(server.name, 'failed to restore session cookie', errMessage);
    }
  }
}
