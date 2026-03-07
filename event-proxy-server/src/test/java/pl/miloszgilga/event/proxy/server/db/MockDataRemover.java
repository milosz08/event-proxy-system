package pl.miloszgilga.event.proxy.server.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.http.EventTableSource;

public class MockDataRemover extends MockData {
  private static final Logger LOG = LoggerFactory.getLogger(MockDataRemover.class);

  public static void main(String[] args) {
    final MockDataRemover mockDataRemover = new MockDataRemover();
    mockDataRemover.remove();
  }

  private void remove() {
    LOG.info("Starting removal of mock data with suffix: {}", MOCK_DATA_SUFFIX);
    final String[] mockSources = {"DLINK", "DVR", "SYSTEM"};
    for (final EventTableSource table : EventTableSource.values()) {
      LOG.info("Cleaning table: {}", table.getTableName());
      for (final String source : mockSources) {
        eventDao.deleteAllByOptionalEventSource(table, parseSourceName(source));
      }
    }
    LOG.info("Cleanup completed");
  }
}
