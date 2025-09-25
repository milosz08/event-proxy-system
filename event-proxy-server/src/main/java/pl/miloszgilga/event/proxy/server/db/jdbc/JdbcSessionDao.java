package pl.miloszgilga.event.proxy.server.db.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.Utils;
import pl.miloszgilga.event.proxy.server.db.DbConnectionPool;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
          session_id TEXT PRIMARY KEY NOT NULL,
          client_id TEXT NOT NULL,
          public_key TEXT NOT NULL,
          public_key_sha256 TEXT NOT NULL UNIQUE,
          expired_at TEXT NOT NULL
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
  public String createSession(String clientId, String publicKey, Duration sessionTime) {
    final String sessionId = UUID.randomUUID().toString();
    final String publicKeySha256 = Utils.generateSha256(publicKey);
    final Instant expiresAt = Instant.now().plus(sessionTime);

    final String sql = String.format("""
        INSERT INTO `%s` (
          session_id, client_id, public_key, public_key_sha256, expired_at
        ) VALUES (?,?,?,?,?,?,?);
      """, TABLE_NAME);

    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, sessionId);
      ps.setString(2, clientId);
      ps.setString(3, publicKey);
      ps.setString(4, publicKeySha256);
      ps.setString(5, DateTimeFormatter.ISO_INSTANT.format(expiresAt));
      final int affectedRows = ps.executeUpdate();
      if (affectedRows > 0) {
        LOG.info("Created session for client: {} with session id: {}", clientId, sessionId);
      }
    } catch (SQLException ex) {
      LOG.error("Unable to create session for client: {}. Cause: {}", clientId, ex.getMessage());
    }
    return sessionId;
  }

  @Override
  public Instant updateSessionTime(String sessionId, Duration sessionTime) {
    final Instant now = Instant.now();
    final Instant newExpiresAt = now.plus(sessionTime);

    final String sql = String.format(
      "UPDATE `%s` SET expired_at = ? WHERE session_id = ?;",
      TABLE_NAME
    );
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, DateTimeFormatter.ISO_INSTANT.format(newExpiresAt));
      ps.setString(2, sessionId);
      final int affectedRows = ps.executeUpdate();
      if (affectedRows > 0) {
        LOG.info("Updated session time with session id: {}", sessionId);
      }
    } catch (SQLException ex) {
      LOG.error("Unable to update session time for session with id: {}. Cause: {}", sessionId,
        ex.getMessage());
    }
    return newExpiresAt;
  }

  @Override
  public void destroySession(String sessionId) {
    final String sql = String.format("DELETE FROM `%s` WHERE session_id = ?;", TABLE_NAME);
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
}
