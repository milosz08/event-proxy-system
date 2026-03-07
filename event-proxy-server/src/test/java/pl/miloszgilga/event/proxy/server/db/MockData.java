package pl.miloszgilga.event.proxy.server.db;

import pl.miloszgilga.event.proxy.server.TestData;
import pl.miloszgilga.event.proxy.server.db.jdbc.JdbcEventDao;

abstract class MockData {
  protected static final String DB_NAME = "events.db";
  protected static final String MOCK_DATA_SUFFIX = "(mock)";

  protected final JdbcEventDao eventDao;

  protected MockData() {
    final DbConnectionPool pool = DbConnectionPool.getInstance(DB_NAME, 5);
    eventDao = new JdbcEventDao(pool);
    eventDao.init();
  }

  protected String parseSourceName(String name) {
    return TestData.generateMockSuffix(name, MOCK_DATA_SUFFIX);
  }
}
