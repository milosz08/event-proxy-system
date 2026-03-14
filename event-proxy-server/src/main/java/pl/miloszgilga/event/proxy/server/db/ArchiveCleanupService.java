package pl.miloszgilga.event.proxy.server.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.AppConfig;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.http.EventTableSource;
import pl.miloszgilga.event.proxy.server.registry.ContentInitializer;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ArchiveCleanupService implements ContentInitializer, Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(ArchiveCleanupService.class);

  private final AppConfig appConfig;
  private final EventDao eventDao;
  private final ScheduledExecutorService scheduler;

  public ArchiveCleanupService(AppConfig appConfig, EventDao eventDao) {
    this.appConfig = appConfig;
    this.eventDao = eventDao;
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      final Thread thread = new Thread(r, "ArchiveCleanup-Thread");
      thread.setDaemon(true);
      return thread;
    });
  }

  @Override
  public void init() {
    final long intervalSec = appConfig.getAsLong(AppConfig.Prop.ARCHIVE_CLEAR_INTERVAL_SEC);
    final long retentionDays = appConfig.getAsLong(AppConfig.Prop.ARCHIVE_RETENTION_DAYS);
    if (intervalSec <= 0 || retentionDays <= 0) {
      LOG.warn("Archive cleanup task is DISABLED (interval or retention is 0)");
      return;
    }
    LOG.info("Starting archive cleanup task. Interval: {}s, Retention: {} days", intervalSec,
      retentionDays);
    scheduler.scheduleAtFixedRate(() -> executeCleanup(retentionDays),
      intervalSec, intervalSec, TimeUnit.SECONDS);

  }

  private void executeCleanup(long retentionDays) {
    LOG.debug("Running scheduled archive cleanup...");
    final long thresholdMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays);
    final int deletedRows = eventDao.deleteRecordsOlderThan(EventTableSource.EVENTS_ARCHIVE,
      thresholdMillis);
    if (deletedRows < 0) {
      LOG.warn("Archive cleanup encountered an error.");
    }
  }

  @Override
  public void close() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
  }
}
