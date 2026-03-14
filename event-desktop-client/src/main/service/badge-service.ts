import { BrowserWindow, app } from 'electron';
import { Badge } from '../badge';
import { DEFAULT_TITLE } from '../index';
import { EventService } from './event-service';

export class BadgeService {
  private unreadCounts = new Map<string, number>();
  private badgeGenerator: Badge;
  private isReady = false;

  private mainWindow: BrowserWindow | null = null;
  private eventService: EventService | null = null;

  constructor(private readonly onBadgeChange?: (counts: Record<string, number>) => void) {
    this.badgeGenerator = new Badge();
    if (process.platform !== 'darwin' && process.platform !== 'linux') {
      this.badgeGenerator.preloadBadges();
    }
  }

  public setReady(): void {
    this.isReady = true;
    this.refreshGlobalBadge();
  }

  public setMainWindow(window: BrowserWindow): void {
    this.mainWindow = window;
  }

  public setEventService(eventService: EventService): void {
    this.eventService = eventService;
  }

  public getUnreadCounts(): Map<string, number> {
    return this.unreadCounts;
  }

  public async syncServerBadgeCount(serverId: string): Promise<void> {
    if (!this.eventService) {
      return;
    }
    const { success, data } = await this.eventService.getCountOfUnreadEvents(serverId, 'EVENTS');
    if (success && data) {
      this.setServerCount(serverId, data.count);
    }
  }

  public initServer(serverId: string): void {
    this.unreadCounts.set(serverId, 0);
  }

  public async withBadgeSync<T>(serverId: string, action: () => Promise<T>): Promise<T> {
    const result = await action();
    await this.syncServerBadgeCount(serverId);
    return result;
  }

  public setServerCount(serverId: string, count: number): void {
    if (!this.unreadCounts.has(serverId)) {
      return;
    }
    const currentCount = this.unreadCounts.get(serverId);
    if (currentCount === count) {
      return;
    }
    this.unreadCounts.set(serverId, count);
    this.refreshGlobalBadge();
  }

  public incrementServerCount(serverId: string, amount: number = 1): void {
    if (!this.unreadCounts.has(serverId)) {
      return;
    }
    const current = this.unreadCounts.get(serverId) || 0;
    this.unreadCounts.set(serverId, current + amount);
    this.refreshGlobalBadge();
  }

  public removeServer(serverId: string): void {
    this.unreadCounts.delete(serverId);
    this.refreshGlobalBadge();
  }

  private refreshGlobalBadge(): void {
    let total = 0;
    let serversWithUnread = 0;
    for (const count of this.unreadCounts.values()) {
      total += count;
      if (count > 0) {
        serversWithUnread++;
      }
    }
    if (!this.isReady) {
      return;
    }
    if (process.platform === 'darwin' || process.platform === 'linux') {
      app.badgeCount = total;
    } else if (this.mainWindow) {
      const { nativeImage, description } = this.badgeGenerator.takeCachedBadge(total);
      this.mainWindow.setOverlayIcon(nativeImage, description);
    }
    if (!this.mainWindow) {
      return;
    }
    this.mainWindow.setTitle(
      total > 0
        ? `${DEFAULT_TITLE} (${total} unread events from ${serversWithUnread} server/s)`
        : DEFAULT_TITLE
    );
    if (this.onBadgeChange) {
      this.onBadgeChange(Object.fromEntries(this.unreadCounts));
    }
  }
}
