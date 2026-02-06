import type { UUID } from 'crypto';
import { safeStorage } from 'electron';
import type { ServerConfig } from './store';
import store from './store';

export class ServerConfigService {
  public getServers(): ServerConfig[] {
    return store.get('servers');
  }

  public getServerById(serverId: string): ServerConfig | undefined {
    return this.getServers().find(s => s.id === serverId);
  }

  public addServer(name: string, url: string, username: string, password: string): UUID {
    const encryptedPassword = safeStorage.isEncryptionAvailable()
      ? safeStorage.encryptString(password).toString('base64')
      : Buffer.from(password).toString('base64');

    const newServer = {
      id: crypto.randomUUID(),
      name,
      url: url.replace(/\/$/, ''),
      username,
      encryptedPassword,
      unreadNotifications: 0,
    };

    const currentServers = this.getServers();
    store.set('servers', [...currentServers, newServer]);
    return newServer.id;
  }

  public removeServer(serverId: string): void {
    const servers = this.getServers();
    const newServers = servers.filter(s => s.id !== serverId);
    store.set('servers', newServers);
  }

  public updateServerSessionData(
    serverId: string,
    sessionCookieJson: string | undefined,
    hasDefaultPassword: boolean | undefined
  ): void {
    this.safetyUpdateServer(serverId, server => {
      if (sessionCookieJson !== undefined) {
        server.sessionCookie = sessionCookieJson;
      }
      if (hasDefaultPassword !== undefined) {
        server.hasDefaultPassword = hasDefaultPassword;
      }
    });
  }

  public getDecryptedPassword(server: ServerConfig): string {
    return safeStorage.isEncryptionAvailable()
      ? safeStorage.decryptString(Buffer.from(server.encryptedPassword, 'base64'))
      : Buffer.from(server.encryptedPassword, 'base64').toString();
  }

  public updateServerPassword(serverId: string, newPassword: string): void {
    this.safetyUpdateServer(serverId, server => {
      server.encryptedPassword = safeStorage.isEncryptionAvailable()
        ? safeStorage.encryptString(newPassword).toString('base64')
        : Buffer.from(newPassword).toString('base64');
      server.hasDefaultPassword = false;
    });
  }

  private safetyUpdateServer(serverId: string, callback: (server: ServerConfig) => void): void {
    const servers = this.getServers();
    const index = servers.findIndex(s => s.id === serverId);
    if (index !== -1) {
      callback(servers[index]);
      store.set('servers', servers);
    }
  }
}
