import { IpcRendererEvent, contextBridge, ipcRenderer } from 'electron';
import { LoginResult, ResponseResult, ServerConfigDTO, ServerInput } from '../@types/shared';

const api = {
  // servers
  addServer: (data: ServerInput): Promise<string> => {
    return ipcRenderer.invoke('server:add', data);
  },
  getServers: (): Promise<ServerConfigDTO[]> => {
    return ipcRenderer.invoke('server:get-all');
  },
  removeServer: (serverId: string): Promise<ResponseResult> => {
    return ipcRenderer.invoke('server:remove', serverId);
  },
  // auth
  connect: (serverId: string): Promise<LoginResult> => {
    return ipcRenderer.invoke('auth:connect', serverId);
  },
  disconnect: (serverId: string): Promise<boolean> => {
    return ipcRenderer.invoke('auth:disconnect', serverId);
  },
  updateDefaultPassword: (serverId: string, newPassword: string): Promise<ResponseResult> => {
    return ipcRenderer.invoke('auth:update-password', { serverId, newPassword });
  },
  onSessionExpired: (callback: (serverId: string) => void) => {
    const listener = (_: IpcRendererEvent, serverId: string): void => callback(serverId);
    ipcRenderer.on('auth:session-expired', listener);
    return () => {
      ipcRenderer.removeListener('auth:session-expired', listener);
    };
  },
  onActiveSessions: (callback: (serverIds: string[]) => void) => {
    const listener = (_: IpcRendererEvent, serverIds: string[]): void => callback(serverIds);
    ipcRenderer.on('auth:active-sessions', listener);
    return () => {
      ipcRenderer.removeListener('auth:active-sessions', listener);
    };
  },
};

contextBridge.exposeInMainWorld('api', api);
