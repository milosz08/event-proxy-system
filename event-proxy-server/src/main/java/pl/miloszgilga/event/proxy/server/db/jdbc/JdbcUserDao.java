package pl.miloszgilga.event.proxy.server.db.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.db.DbConnectionPool;
import pl.miloszgilga.event.proxy.server.db.dao.UserDao;

import java.sql.*;

public class JdbcUserDao implements UserDao {
  public static final String TABLE_NAME = "_users";
  private static final Logger LOG = LoggerFactory.getLogger(JdbcUserDao.class);
  private final DbConnectionPool dbConnectionPool;

  public JdbcUserDao(DbConnectionPool dbConnectionPool) {
    this.dbConnectionPool = dbConnectionPool;
  }

  @Override
  public void init() {
    final String sql = String.format("""
      CREATE TABLE IF NOT EXISTS `%s` (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT NOT NULL UNIQUE,
        password TEXT NOT NULL,
        defaultPassword INTEGER NOT NULL DEFAULT 1
      );
      """, TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final Statement statement = conn.createStatement()) {
      statement.execute(sql);
      LOG.info("Init table (or skip): {}", TABLE_NAME);
    } catch (SQLException ex) {
      LOG.error("Unable to create table: {}. Cause: {}", TABLE_NAME, ex.getMessage());
    }
  }

  @Override
  public String getUserPasswordHash(String username) {
    final String sql = String.format("SELECT password FROM `%s` WHERE username = ?;", TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (final ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getString(1);
        }
      }
    } catch (SQLException ex) {
      LOG.error("Unable to get password hash from user with username: {}. Cause: {}", username,
        ex.getMessage());
    }
    return null;
  }

  @Override
  public Integer getUserId(String username) {
    final String sql = String.format("SELECT id FROM `%s` WHERE username = ?;", TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (final ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
      }
    } catch (SQLException ex) {
      LOG.error("Unable to get user id from user with username: {}. Cause: {}", username,
        ex.getMessage());
    }
    return null;
  }

  @Override
  public Boolean userExists(String username) {
    final String sql = String.format("""
      SELECT COUNT(*) > 0 FROM `%s` WHERE username = ?;
      """, TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (final ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getBoolean(1);
        }
      }
    } catch (SQLException ignored) {
    }
    return false;
  }

  @Override
  public void createUser(String username, String hashedDefaultPassword) {
    final String sql = String.format("""
        INSERT INTO `%s` (username, password) VALUES (?,?);
      """, TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      ps.setString(2, hashedDefaultPassword);
      final int affectedRows = ps.executeUpdate();
      if (affectedRows > 0) {
        LOG.info("Created user with username: {}", username);
      }
    } catch (SQLException ex) {
      LOG.error("Unable to create user with username: {}. Cause: {}", username, ex.getMessage());
    }
  }

  @Override
  public void updateUserPassword(String username, String newHashedPassword,
                                 boolean defaultPassword) {
    final String sql = String.format(
      "UPDATE `%s` SET password = ?, defaultPassword = ? WHERE username = ?;",
      TABLE_NAME
    );
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, newHashedPassword);
      ps.setInt(2, defaultPassword ? 1 : 0);
      ps.setString(3, username);
      final int affectedRows = ps.executeUpdate();
      if (affectedRows > 0) {
        LOG.info("Updated password for user with username: {}", username);
      }
    } catch (SQLException ex) {
      LOG.error("Unable to update password for user with username: {}. Cause: {}",
        username, ex.getMessage());
    }
  }

  @Override
  public Boolean userHasDefaultPassword(String username) {
    final String sql = String.format("""
      SELECT defaultPassword FROM `%s` WHERE username = ?;
      """, TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (final ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) == 1;
        }
      }
    } catch (SQLException ex) {
      LOG.error("Unable to get default password info from user with username: {}. Cause: {}",
        username, ex.getMessage());
    }
    return null;
  }

  @Override
  public void deleteUsers() {
    final String sql = String.format("DELETE FROM `%s`;", TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final Statement statement = conn.createStatement()) {
      final int affectedRows = statement.executeUpdate(sql);
      LOG.warn("Deleted all users ({}) from table: {}", affectedRows, TABLE_NAME);
    } catch (SQLException ex) {
      LOG.error("Unable to delete all users table: {}. Cause: {}", TABLE_NAME, ex.getMessage());
    }
  }
}
