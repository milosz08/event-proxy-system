import { IpcRendererEvent, contextBridge, ipcRenderer } from 'electron';

const api = {
  sendPing: () => ipcRenderer.send('app:ping'),
  clearPings: () => ipcRenderer.send('app:clearPings'),
  onPong: (callback: (message: string) => void) => {
    const listener = (_: IpcRendererEvent, message: string): void => callback(message);
    ipcRenderer.on('app:pong', listener);
    return () => {
      ipcRenderer.removeListener('app:pong', listener);
    };
  },
  onClearedPings: (callback: () => void) => {
    const listener = (_: IpcRendererEvent): void => callback();
    ipcRenderer.on('app:clearedPings', listener);
    return () => {
      ipcRenderer.removeListener('app:clearedPings', listener);
    };
  }
};

contextBridge.exposeInMainWorld('api', api);
