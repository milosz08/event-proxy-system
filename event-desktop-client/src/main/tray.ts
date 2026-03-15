import { BrowserWindow, Menu, Tray } from 'electron';
import icon from '../../resources/icon.png?asset';
import { Badge } from './badge';
import { DEFAULT_TITLE } from './index';
import store from './store';

export interface TrayServerData {
  id: string;
  name: string;
  isConnected: boolean;
  unreadCount: number;
}

const TrayIcon = {
  ONLINE: '🟢',
  OFFLINE: '🔴',
} as const;

export class MenuTray {
  private tray: Tray | undefined;

  constructor(
    private readonly mainWindow: BrowserWindow,
    private readonly badge: Badge,
    private readonly onClose: () => void
  ) {}

  public createTray(): void {
    if (this.tray) {
      return;
    }
    this.tray = new Tray(icon);
    this.tray.setToolTip(DEFAULT_TITLE);
    this.tray.setImage(this.badge.getTrayIcon(false));
    this.tray.on('double-click', () => {
      if (this.mainWindow) {
        this.mainWindow.show();
      }
    });
    this.updateTray([]);
  }

  public updateTray(trayServers: TrayServerData[]): void {
    if (!this.tray) {
      return;
    }
    const trayIcon = this.badge.getTrayIcon(trayServers.some(({ unreadCount }) => unreadCount > 0));
    const template: Electron.MenuItemConstructorOptions[] = [
      {
        label: ' Event desktop client',
        icon: trayIcon.resize({ width: 16, height: 16 }),
        enabled: false,
      },
      { type: 'separator' },
    ];
    if (trayServers.length === 0) {
      template.push({ label: 'No servers', enabled: false });
    } else {
      trayServers.forEach(({ name, isConnected, unreadCount }) => {
        const statusIcon = isConnected ? TrayIcon.ONLINE : TrayIcon.OFFLINE;
        template.push({
          label: `(${unreadCount}) ${statusIcon} ${name}`,
          click: () => this.mainWindow.show(),
        });
      });
    }
    template.push(
      { type: 'separator' },
      {
        label: 'Show app',
        click: () => this.mainWindow.show(),
      },
      {
        label: 'Close, without hiding',
        type: 'checkbox',
        checked: store.get('closeAppCompletely'),
        click: menuItem => {
          store.set('closeAppCompletely', menuItem.checked);
        },
      },
      { type: 'separator' },
      {
        label: 'Exit',
        click: () => this.onClose(),
      }
    );
    const contextMenu = Menu.buildFromTemplate(template);
    this.tray.setImage(trayIcon);
    this.tray.setContextMenu(contextMenu);
  }
}
