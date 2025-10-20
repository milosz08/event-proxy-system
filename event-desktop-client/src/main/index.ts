import { electronApp, is, optimizer } from '@electron-toolkit/utils';
import { BrowserWindow, IpcMainEvent, app, ipcMain, shell } from 'electron';
import { join } from 'path';
import icon from '../../resources/icon.png?asset';
import { Badge } from './badge';
import { logger } from "./logger";

const electronRendererUrl = process.env['ELECTRON_RENDERER_URL'];
const appId = 'pl.miloszgilga.event-proxy-client';

const createWindow = async (): Promise<BrowserWindow> => {
  const mainWindow = new BrowserWindow({
    width: 1280,
    height: 720,
    show: false,
    title: 'Event desktop client',
    autoHideMenuBar: true,
    ...(process.platform === 'linux' || !app.isPackaged ? { icon } : {}),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false,
    },
  });

  mainWindow.on('ready-to-show', () => mainWindow.show());

  mainWindow.webContents.setWindowOpenHandler(details => {
    shell.openExternal(details.url).then(r => r);
    return { action: 'deny' };
  });

  if (is.dev && electronRendererUrl) {
    await mainWindow.loadURL(electronRendererUrl);
  } else {
    await mainWindow.loadFile(join(__dirname, '../renderer/index.html'));
  }
  return mainWindow;
};

const onReady = async (): Promise<void> => {
  if (process.platform === 'win32') {
    electronApp.setAppUserModelId(appId);
  }

  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window);
  });

  const mainWindow = await createWindow();

  const badge = new Badge();
  badge.preloadBadges();

  let notificationsCounter = 0;

  ipcMain.on('app:ping', (event: IpcMainEvent) => {
    console.log('received ping event from client');

    const { nativeImage, description } = badge.takeCachedBadge(++notificationsCounter);
    mainWindow.setOverlayIcon(nativeImage, description);

    event.sender.send('app:pong', 'this is pong from main process send via IPC from renderer!');
  });

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
};

const onClose = (): void => {
  app.quit();
};

app.whenReady().then(onReady);
app.on('window-all-closed', onClose);
