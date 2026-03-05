import {
  LoginResult,
  ResponseResult,
  ServerConfigDTO,
  ServerInput,
  SseEventMessage,
} from '../@types/shared';

declare global {
  interface Window {
    api: {
      // servers
      addServer: (data: ServerInput) => Promise<string>;
      getServers: () => Promise<ServerConfigDTO[]>;
      removeServer: (serverId: string) => Promise<ResponseResult>;
      onHeartbeat: (
        callback: (serverId: string, status: boolean, resTimeMillis?: number) => void
      ) => () => void;
      // sse
      onSseMessage: (
        callback: (
          serverId: string,
          payload: SseEventMessage | undefined,
          error: string | undefined
        ) => void
      ) => () => void;
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
