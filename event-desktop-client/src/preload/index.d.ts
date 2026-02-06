import { LoginResult, ResponseResult, ServerConfigDTO, ServerInput } from '../@types/shared';

declare global {
  interface Window {
    api: {
      // servers
      addServer: (data: ServerInput) => Promise<string>;
      getServers: () => Promise<ServerConfigDTO[]>;
      removeServer: (serverId: string) => Promise<ResponseResult>;
      // auth
      connect: (serverId: string) => Promise<LoginResult>;
      disconnect: (serverId: string) => Promise<boolean>;
      updateDefaultPassword: (serverId: string, newPassword: string) => Promise<ResponseResult>;
      onSessionExpired: (callback: (serverId: string) => void) => () => void;
      onActiveSessions: (callback: (serverIds: string[]) => void) => () => void;
    };
  }
}

export {};
