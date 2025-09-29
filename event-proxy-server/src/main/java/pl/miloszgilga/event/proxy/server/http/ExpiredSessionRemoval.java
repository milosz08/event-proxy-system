package pl.miloszgilga.event.proxy.server.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;
import pl.miloszgilga.event.proxy.server.registry.ContentInitializer;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExpiredSessionRemoval implements ContentInitializer, Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(ExpiredSessionRemoval.class);

  private final SessionDao sessionDao;
  private final int intervalSec;
  private final ScheduledExecutorService scheduler;

  public ExpiredSessionRemoval(SessionDao sessionDao, int intervalSec) {
    this.sessionDao = sessionDao;
    this.intervalSec = intervalSec;
    scheduler = Executors.newSingleThreadScheduledExecutor();
  }

  @Override
  public void init() {
    scheduler.scheduleAtFixedRate(sessionDao::removeExpired, 0, intervalSec, TimeUnit.SECONDS);
    LOG.info("Started expired session removal executor service");
  }

  @Override
  public void close() {
    scheduler.shutdown();
  }
}
