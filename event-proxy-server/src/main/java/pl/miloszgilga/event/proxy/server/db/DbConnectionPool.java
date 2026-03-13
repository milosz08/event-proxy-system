package pl.miloszgilga.event.proxy.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class DbConnectionPool {
  private static final Logger LOG = LoggerFactory.getLogger(DbConnectionPool.class);

  private volatile static DbConnectionPool instance;
  private final HikariDataSource dataSource;

  private DbConnectionPool(String dbName, int maximumPoolSize) {
    createMissingDir(dbName);
    final HikariConfig config = new HikariConfig();

    config.setJdbcUrl("jdbc:sqlite:" + dbName);
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

    config.setMaximumPoolSize(maximumPoolSize);

    dataSource = new HikariDataSource(config);
  }

  public static DbConnectionPool getInstance(String dbName, int maximumPoolSize) {
    if (instance == null) {
      synchronized (DbConnectionPool.class) {
        if (instance == null) {
          instance = new DbConnectionPool(dbName, maximumPoolSize);
        }
      }
    }
    return instance;
  }

  private void createMissingDir(String dbName) {
    final File dbFile = new File(dbName);
    final File parentDir = dbFile.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      if (parentDir.mkdirs()) {
        LOG.info("Created missing directory structure: {}", parentDir.getPath());
      }
    }
  }

  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }
}
