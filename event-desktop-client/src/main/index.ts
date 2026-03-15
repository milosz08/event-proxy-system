import { electronApp, is, optimizer } from '@electron-toolkit/utils';
import { BrowserWindow, app, ipcMain, shell } from 'electron';
import { join } from 'path';
import icon from '../../resources/icon.png?asset';
import { UiConfig } from '../@types/shared';
import { Badge } from './badge';
import { NetworkSessionManager } from './network/network-session-manager';
import { SessionHeartbeatService } from './network/session-heartbeat-service';
import { AuthService } from './service/auth-service';
import { BadgeService } from './service/badge-service';
import { ConfigService } from './service/config-service';
import { CryptoService } from './service/crypto-service';
import { EventService } from './service/event-service';
import store from './store';
import { MenuTray } from './tray';

const electronRendererUrl = process.env['ELECTRON_RENDERER_URL'];
const appId = 'pl.miloszgilga.event-proxy-client';

export const DEFAULT_TITLE = 'Event desktop client';
const MIN_WIDTH = 1280;
const MIN_HEIGHT = 720;

let isQuitting = false;
let updateTrayMenu: () => void;

const createWindow = async (): Promise<BrowserWindow> => {
  const mainWindow = new BrowserWindow({
    width: MIN_WIDTH,
    height: MIN_HEIGHT,
    minWidth: MIN_WIDTH,
    minHeight: MIN_HEIGHT,
    show: false,
    title: DEFAULT_TITLE,
    autoHideMenuBar: true,
    backgroundColor: '#2f343c',
    ...(process.platform === 'linux' || !app.isPackaged ? { icon } : {}),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false,
    },
  });

  mainWindow.on('ready-to-show', () => {
    if (!process.argv.includes('--hidden')) {
      mainWindow.show();
    }
  });

  mainWindow.webContents.setWindowOpenHandler(details => {
    shell.openExternal(details.url).then(r => r);
    return { action: 'deny' };
  });

  mainWindow.on('close', event => {
    const closeAppCompletely = store.get('closeAppCompletely');
    if (!isQuitting && !closeAppCompletely) {
      event.preventDefault();
      mainWindow.hide();
    }
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
  const shouldOpenAtLogin = store.get('openAtLogin');
  app.setLoginItemSettings({
    openAtLogin: shouldOpenAtLogin,
    path: app.getPath('exe'),
    args: shouldOpenAtLogin ? ['--hidden'] : [],
  });

  let resolveBoot: () => void;
  const bootPromise = new Promise<void>(resolve => {
    resolveBoot = resolve;
  });

  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window);
  });

  const mainWindow = await createWindow();
  mainWindow.on('page-title-updated', evt => {
    evt.preventDefault();
  });

  // badge
  const badge = new Badge();
  const badgeService = new BadgeService(badge, counts => {
    mainWindow.webContents.send('badge:sync-all', counts);
    if (updateTrayMenu) {
      updateTrayMenu();
    }
  });
  await badgeService.preloadBadges(icon);
  badgeService.setMainWindow(mainWindow);

  mainWindow.on('show', () => badgeService.refresh());

  const cryptoService = new CryptoService();
  const configService = new ConfigService();

  // menu tray
  const menuTray = new MenuTray(mainWindow, badge, configService, () => {
    isQuitting = true;
    app.quit();
  });
  menuTray.createTray();

  const networkManager = new NetworkSessionManager(configService);
  const heartbeatService = new SessionHeartbeatService();
  const eventService = new EventService(configService, networkManager, cryptoService, {
    onEvent: (serverId, payload) => {
      badgeService.incrementServerCount(serverId);
      mainWindow.webContents.send('sse:event', serverId, payload);
    },
  });
  badgeService.setEventService(eventService);
  updateTrayMenu = () => {
    menuTray.updateTray(
      configService.getServers().map(server => ({
        id: server.id,
        name: server.name,
        isConnected: authService.getActiveSessionIds().includes(server.id),
        unreadCount: badgeService.getUnreadCounts().get(server.id) || 0,
      }))
    );
  };
  const authService = new AuthService(configService, networkManager, heartbeatService, {
    onHeartbeat: (serverId, status, resTimeMillis) => {
      heartbeatService.updateLastStatus(serverId, status, resTimeMillis);
      mainWindow.webContents.send('server:heartbeat', serverId, status, resTimeMillis);
    },
    onSessionExpired: serverId => {
      badgeService.removeServer(serverId);
      mainWindow.webContents.send('auth:session-expired', serverId);
      updateTrayMenu();
    },
    onConnect: async server => {
      badgeService.initServer(server.id);
      await badgeService.syncServerBadgeCount(server.id);
      await eventService.startEventsStream(server.id);
    },
    onDisconnect: async server => {
      eventService.stopEventsStream(server.id);
      badgeService.removeServer(server.id);
      updateTrayMenu();
    },
    onSetupPoint: async (serverName, msg) => {
      mainWindow.webContents.send('status:setup-point', serverName, msg);
    },
  });

  // ipc badge
  ipcMain.handle('badge:get-all-counts', () => {
    return Object.fromEntries(badgeService.getUnreadCounts());
  });

  // ipc servers
  ipcMain.handle('server:add', async (_, data) => {
    return configService.addServer(data.name, data.url, data.username, data.password);
  });
  ipcMain.handle('server:get-all', () => {
    return configService.getServers().map(server => {
      const lastHeartbeat = heartbeatService.getLastStatus(server.id);
      return {
        ...server,
        lastHeartbeatStatus: lastHeartbeat?.status ?? false,
        lastHeartbeatResTimeMillis: lastHeartbeat?.resTimeMillis,
        lastHeartbeatTimestamp: lastHeartbeat?.timestamp,
      };
    });
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
  ipcMain.handle('auth:update-password', async (_, { serverId, newPassword }) => {
    return await authService.updateDefaultPassword(serverId, newPassword);
  });
  ipcMain.handle('auth:get-initial-sessions', async () => {
    await bootPromise;
    return authService.getActiveSessionIds();
  });

  // ipc ui config
  ipcMain.handle('ui-config:get', () => {
    return configService.getUiConfig();
  });
  ipcMain.handle('ui-config:set', (_, uiConfig: Partial<UiConfig>) => {
    return configService.updateUiConfig(uiConfig);
  });

  // ipc event source
  ipcMain.handle('event-source:all', async (_, { serverId, eventTable }) => {
    return await eventService.getEventSources(serverId, eventTable);
  });

  // ipc events
  ipcMain.handle(
    'events:pageable-all',
    (_, { serverId, eventTable, subjectSearch, isAscending, offset, limit, eventSource }) => {
      return eventService.getPageableEvents(
        serverId,
        eventTable,
        subjectSearch,
        isAscending,
        offset,
        limit,
        eventSource
      );
    }
  );
  ipcMain.handle('events:details-single', (_, { serverId, eventTable, eventId }) => {
    return eventService.getEventDetails(serverId, eventTable, eventId);
  });
  ipcMain.handle('events:mark-as-read-single', async (_, { serverId, eventTable, eventId }) => {
    return await badgeService.withBadgeSync(serverId, () =>
      eventService.markEventAsRead(serverId, eventTable, eventId)
    );
  });
  ipcMain.handle('events:mark-as-unread-single', async (_, { serverId, eventTable, eventId }) => {
    return await badgeService.withBadgeSync(serverId, () =>
      eventService.markEventAsUnread(serverId, eventTable, eventId)
    );
  });
  ipcMain.handle('events:archive-bulk', async (_, { serverId, eventIds }) => {
    return await badgeService.withBadgeSync(serverId, () =>
      eventService.bulkArchiveEvents(serverId, eventIds)
    );
  });
  ipcMain.handle('events:unarchive-bulk', async (_, { serverId, eventIds }) => {
    return await badgeService.withBadgeSync(serverId, () =>
      eventService.bulkUnarchiveEvents(serverId, eventIds)
    );
  });
  ipcMain.handle('events:delete-bulk', async (_, { serverId, eventTable, eventIds }) => {
    return await badgeService.withBadgeSync(serverId, () =>
      eventService.bulkDeleteEvents(serverId, eventTable, eventIds)
    );
  });
  ipcMain.handle('events:archive-all', async (_, { serverId, eventSource }) => {
    return await badgeService.withBadgeSync(serverId, () =>
      eventService.allArchiveEvents(serverId, eventSource)
    );
  });
  ipcMain.handle('events:unarchive-all', async (_, { serverId, eventSource }) => {
    return await badgeService.withBadgeSync(serverId, () =>
      eventService.allUnarchiveEvents(serverId, eventSource)
    );
  });
  ipcMain.handle('events:delete-all', async (_, { serverId, eventTable, eventSource }) => {
    return await badgeService.withBadgeSync(serverId, () =>
      eventService.allDeleteEvents(serverId, eventTable, eventSource)
    );
  });

  await authService.autoLogin();
  badgeService.setReady();
  resolveBoot!();

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
