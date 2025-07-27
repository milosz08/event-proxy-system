package pl.miloszgilga.event.proxy.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

class DbConnectionPool {
  private final HikariDataSource dataSource;

  private volatile static DbConnectionPool instance;

  private DbConnectionPool(String dbName, int maximumPoolSize) {
    final HikariConfig config = new HikariConfig();

    config.setJdbcUrl("jdbc:sqlite:" + dbName);
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

    config.setMaximumPoolSize(maximumPoolSize);

    dataSource = new HikariDataSource(config);
  }

  Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  static DbConnectionPool getInstance(String dbName, int maximumPoolSize) {
    if (instance == null) {
      synchronized (DbConnectionPool.class) {
        if (instance == null) {
          instance = new DbConnectionPool(dbName, maximumPoolSize);
        }
      }
    }
    return instance;
  }
}
