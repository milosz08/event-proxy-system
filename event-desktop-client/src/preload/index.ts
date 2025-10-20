import { IpcRendererEvent, contextBridge, ipcRenderer } from 'electron';

const api = {
  sendPing: () => ipcRenderer.send('app:ping'),
  onPong: (callback: (message: string) => void) => {
    const listener = (_: IpcRendererEvent, message: string): void => {
      callback(message);
    };
    ipcRenderer.on('app:pong', listener);
    return () => {
      ipcRenderer.removeListener('app:pong', listener);
    };
  },
};

contextBridge.exposeInMainWorld('api', api);
