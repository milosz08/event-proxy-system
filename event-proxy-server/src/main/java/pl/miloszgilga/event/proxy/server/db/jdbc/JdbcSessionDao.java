package pl.miloszgilga.event.proxy.server.db.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.db.DbConnectionPool;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;
import pl.miloszgilga.event.proxy.server.http.SessionData;

import java.sql.*;
import java.time.Instant;

public class JdbcSessionDao implements SessionDao {
  private static final Logger LOG = LoggerFactory.getLogger(JdbcSessionDao.class);
  private static final String TABLE_NAME = "_sessions";

  private final DbConnectionPool dbConnectionPool;

  public JdbcSessionDao(DbConnectionPool dbConnectionPool) {
    this.dbConnectionPool = dbConnectionPool;
  }

  @Override
  public void init() {
    // generate table for storing client session info, only one time (including public key incoming
    // with client login request)
    final String sql = String.format("""
        CREATE TABLE IF NOT EXISTS `%s` (
          sessionId TEXT PRIMARY KEY NOT NULL,
          clientId TEXT NOT NULL,
          publicKey TEXT NOT NULL,
          expiredAtUtc DATETIME NOT NULL,
          userId INTEGER NOT NULL,
          FOREIGN KEY(userId) REFERENCES %s(id) ON DELETE CASCADE
        );
      """, TABLE_NAME, JdbcUserDao.TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final Statement statement = conn.createStatement()) {
      statement.execute(sql);
      LOG.info("Init table (or skip): {}", TABLE_NAME);
    } catch (SQLException ex) {
      LOG.error("Unable to create table: {}. Cause: {}", TABLE_NAME, ex.getMessage());
    }
  }

  @Override
  public void createSession(String sessionId, Integer userId, String clientId, String publicKey,
                            Instant expiresAt) {
    final String sql = String.format("""
        INSERT INTO `%s` (sessionId, clientId, publicKey, expiredAtUtc, userId) VALUES (?,?,?,?,?);
      """, TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, sessionId);
      ps.setString(2, clientId);
      ps.setString(3, publicKey);
      ps.setTimestamp(4, Timestamp.from(expiresAt));
      ps.setInt(5, userId);
      final int affectedRows = ps.executeUpdate();
      if (affectedRows > 0) {
        LOG.info("Created session for client: {} with session id: {}", clientId, sessionId);
      }
    } catch (SQLException ex) {
      LOG.error("Unable to create session for client: {}. Cause: {}", clientId, ex.getMessage());
    }
  }

  @Override
  public SessionData getSession(String sessionId) {
    final Instant now = Instant.now();
    final String sql = String.format("""
      SELECT clientId, publicKey, username FROM `%s` s
      INNER JOIN `%s` u ON s.userId = u.id WHERE sessionId = ? AND expiredAtUtc >= ?;
      """, TABLE_NAME, JdbcUserDao.TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, sessionId);
      ps.setTimestamp(2, Timestamp.from(now));
      try (final ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new SessionData(rs.getString(1), rs.getString(2), rs.getString(3));
        }
      }
    } catch (SQLException ex) {
      LOG.error("Unable to get session with id: {}. Cause: {}", sessionId, ex.getMessage());
    }
    return null;
  }

  @Override
  public void updateSessionTime(String sessionId, Instant newExpiresAt) {
    final String sql = String.format(
      "UPDATE `%s` SET expiredAtUtc = ? WHERE sessionId = ?;",
      TABLE_NAME
    );
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setTimestamp(1, Timestamp.from(newExpiresAt));
      ps.setString(2, sessionId);
      final int affectedRows = ps.executeUpdate();
      if (affectedRows > 0) {
        LOG.debug("Updated session time with session id: {}", sessionId);
      }
    } catch (SQLException ex) {
      LOG.error("Unable to update session time for session with id: {}. Cause: {}", sessionId,
        ex.getMessage());
    }
  }

  @Override
  public void destroySession(String sessionId) {
    final String sql = String.format("DELETE FROM `%s` WHERE sessionId = ?;", TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, sessionId);
      final int affectedRows = ps.executeUpdate();
      if (affectedRows > 0) {
        LOG.info("Destroy session with session id: {}", sessionId);
      }
    } catch (SQLException ex) {
      LOG.error("Unable to destroy session with session id: {}. Cause: {}", sessionId,
        ex.getMessage());
    }
  }

  @Override
  public void removeExpired() {
    final String sql = String.format("DELETE FROM `%s` WHERE expiredAtUtc < ?;", TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.executeUpdate();
    } catch (SQLException ignored) {
    }
  }
}
