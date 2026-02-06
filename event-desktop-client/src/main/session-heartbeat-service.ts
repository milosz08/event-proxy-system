import { logger } from './logger';
import { ServerConfig } from './store';
import { extractErrorMessage } from './utils';

export class SessionHeartbeatService {
  private timers: Map<string, NodeJS.Timeout> = new Map();

  public start(
    server: ServerConfig,
    refreshAction: () => Promise<number | false>,
    onFailure: () => Promise<void>,
    intervalMs: number
  ): void {
    this.stop(server);
    const scheduleNext = (delayMs: number): void => {
      const safeDelay = Math.max(delayMs, 20000);
      logger.info(`[${server.name}] starting heartbeat (every ${safeDelay}ms)`);
      const timer = setTimeout(async () => {
        try {
          const nextIntervalOrFalse = await refreshAction();
          if (typeof nextIntervalOrFalse === 'boolean' && !nextIntervalOrFalse) {
            logger.warn(`[${server.name}] heartbeat failed (session invalid)`);
            this.stop(server);
            await onFailure();
          } else {
            scheduleNext(nextIntervalOrFalse as number);
          }
        } catch (err) {
          const errMsg = extractErrorMessage(err);
          logger.error(`[${server.name}] heartbeat execution error:`, errMsg);
          this.stop(server);
        }
      }, safeDelay);
      timer.unref(); // prevent close blocking node process
      this.timers.set(server.id, timer);
    };
    scheduleNext(intervalMs);
  }

  public stop(server: ServerConfig): void {
    if (!this.timers.has(server.id)) {
      return;
    }
    clearTimeout(this.timers.get(server.id));
    this.timers.delete(server.id);
    logger.info(`[${server.name}] heartbeat stopped.`);
  }

  public getActiveIds(): string[] {
    return Array.from(this.timers.keys());
  }
}
