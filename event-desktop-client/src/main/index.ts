import { electronApp, is, optimizer } from '@electron-toolkit/utils';
import { BrowserWindow, app, ipcMain, shell } from 'electron';
import { join } from 'path';
import icon from '../../resources/icon.png?asset';
import { AuthService } from './auth-service';
import { Badge } from './badge';
import { logger } from './logger';
import { NetworkSessionManager } from './network-session-manager';
import { ServerConfigService } from './server-config-service';
import { SessionHeartbeatService } from './session-heartbeat-service';

const electronRendererUrl = process.env['ELECTRON_RENDERER_URL'];
const appId = 'pl.miloszgilga.event-proxy-client';

const createWindow = async (): Promise<BrowserWindow> => {
  const mainWindow = new BrowserWindow({
    width: 1280,
    height: 720,
    minWidth: 1280,
    minHeight: 720,
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

  const configService = new ServerConfigService();
  const networkManager = new NetworkSessionManager(configService);
  const heartbeatService = new SessionHeartbeatService();
  const authService = new AuthService(configService, networkManager, heartbeatService, serverId => {
    if (!mainWindow.isDestroyed()) {
      mainWindow.webContents.send('auth:session-expired', serverId);
    }
  });

  // ipc servers
  ipcMain.handle('server:add', async (_, data) => {
    return configService.addServer(data.name, data.url, data.username, data.password);
  });

  ipcMain.handle('server:get-all', () => {
    return configService.getServers();
  });
  ipcMain.handle('server:remove', async (_, serverId: string) => {
    return await authService.removeServer(serverId);
  });

  // ipc auth
  ipcMain.handle('auth:connect', async (_, serverId: string) => {
    return await authService.connect(serverId);
  });

  ipcMain.handle('auth:disconnect', async (_, serverId: string) => {
    return await authService.disconnect(serverId);
  });

  ipcMain.handle('auth:update-password', async (_, args) => {
    return await authService.updateDefaultPassword(args.serverId, args.newPassword);
  });

  await autoLogin(mainWindow, configService, authService);

  // badge
  const badge = new Badge();
  if (process.platform !== 'darwin' && process.platform !== 'linux') {
    badge.preloadBadges();
  }

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
};

const autoLogin = async (
  mainWindow: BrowserWindow,
  configService: ServerConfigService,
  authService: AuthService
): Promise<void> => {
  const servers = configService.getServers();
  if (servers.length === 0) {
    return;
  }
  logger.info(`checking sessions for ${servers.length} servers...`);
  await Promise.allSettled(
    servers.map(async server => {
      const isValid = await authService.initializeSession(server.id);
      const message = isValid ? 'session active, heartbeat started' : 'session expired or invalid';
      logger.info(`[${server.name}] ${message}`);
    })
  );
  mainWindow.webContents.send('auth:active-sessions', authService.getActiveSessionIds());
};

const onClose = (): void => {
  app.quit();
};

app.whenReady().then(onReady);
app.on('window-all-closed', onClose);
