import { logger } from './logger';
import { ServerConfig } from './store';
import { extractErrorMessage } from './utils';

export class SessionHeartbeatService {
  private timers: Map<string, NodeJS.Timeout> = new Map();

  public start(
    server: ServerConfig,
    refreshAction: () => Promise<number | false>,
    onFailure: () => Promise<void>
  ): void {
    this.stop(server);
    logger.info(`[${server.name}] starting heartbeat loop (immediate execution)`);
    const executeCycle = async (): Promise<void> => {
      try {
        const nextIntervalOrFalse = await refreshAction();
        if (!this.timers.has(server.id)) {
          return;
        }
        if (typeof nextIntervalOrFalse === 'boolean' && !nextIntervalOrFalse) {
          logger.warn(`[${server.name}] heartbeat failed (session invalid)`);
          this.stop(server);
          await onFailure();
        } else {
          const delayMs = nextIntervalOrFalse as number;
          const safeDelay = Math.max(delayMs, 20000);
          logger.info(`[${server.name}] scheduling next heartbeat in ${safeDelay}ms`);

          const nextTimer = setTimeout(executeCycle, safeDelay);
          nextTimer.unref(); // prevent close blocking node process
          this.timers.set(server.id, nextTimer);
        }
      } catch (err) {
        const errMsg = extractErrorMessage(err);
        logger.error(`[${server.name}] heartbeat execution error:`, errMsg);
        this.stop(server);
      }
    };
    const placeholderTimer = setTimeout(() => {}, 0);
    this.timers.set(server.id, placeholderTimer);
    executeCycle().then(r => r);
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
