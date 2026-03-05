package pl.miloszgilga.event.proxy.server.db.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.db.DbConnectionPool;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.db.dto.MessageContent;
import pl.miloszgilga.event.proxy.server.db.dto.MessageContentWithBody;
import pl.miloszgilga.event.proxy.server.http.Page;
import pl.miloszgilga.event.proxy.server.queue.EmailProperties;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JdbcEventDao implements EventDao {
  private static final Logger LOG = LoggerFactory.getLogger(JdbcEventDao.class);
  private static final String TABLE_NAME = "events";

  private final DbConnectionPool dbConnectionPool;

  public JdbcEventDao(DbConnectionPool dbConnectionPool) {
    this.dbConnectionPool = dbConnectionPool;
  }

  @Override
  public void init() {
    final String sql = String.format("""
        CREATE TABLE IF NOT EXISTS `%s` (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          eventSource TEXT NOT NULL,
          subject TEXT NOT NULL,
          rawBody TEXT NOT NULL,
          eventTime TIMESTAMP NOT NULL,
          isUnread INTEGER DEFAULT 0 CHECK (isUnread IN (0, 1))
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
  public Page<MessageContent> getAllByEventSource(String eventSource, int limit, int offset) {
    final List<MessageContent> results = new ArrayList<>();
    long totalElements = 0;

    final String countSql = String.format("SELECT COUNT(*) FROM `%s`;", eventSource);
    final String sql = String.format("""
        SELECT id, subject, eventTime FROM `%s` WHERE eventSource = ?
        ORDER BY id DESC LIMIT ? OFFSET ?;
      """, TABLE_NAME);

    try (final Connection conn = dbConnectionPool.getConnection()) {
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, eventSource);
        ps.setInt(2, limit);
        ps.setInt(3, offset);
        try (final ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            final MessageContent messageContent = new MessageContent(
              rs.getLong("id"),
              rs.getString("subject"),
              rs.getTimestamp("eventTime").toLocalDateTime(),
              rs.getBoolean("isUnread")
            );
            results.add(messageContent);
          }
        }
      }
      try (final Statement st = conn.createStatement();
           final ResultSet rs = st.executeQuery(countSql)) {
        if (rs.next()) {
          totalElements = rs.getLong(1);
        }
      }
    } catch (SQLException ex) {
      LOG.error("Unable to get all records from event source: {}. Cause: {}", eventSource,
        ex.getMessage());
      return new Page<>(Collections.emptyList(), false, 0);
    }
    final boolean hasNext = (long) offset + results.size() < totalElements;
    return new Page<>(results, hasNext, totalElements);
  }

  @Override
  public MessageContentWithBody getSingleById(long id) {
    final String sql = String.format("SELECT * FROM `%s` WHERE id = ?;", TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (final ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new MessageContentWithBody(
            rs.getLong("id"),
            rs.getString("subject"),
            rs.getString("rawBody"),
            rs.getTimestamp("eventTime").toLocalDateTime(),
            rs.getBoolean("isUnread")
          );
        }
      }
    } catch (SQLException ex) {
      LOG.error("Unable to get record by id: {}. Cause: {}", id, ex.getMessage());
    }
    return null;
  }

  @Override
  public boolean eventSourceExists(String eventSource) {
    final String sql = String.format("""
         SELECT COUNT(*) > 0 FROM `%s` WHERE eventSource = ?;
      """, TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventSource);
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
  public boolean makeEventRead(long id) {
    final String sql = String.format("UPDATE `%s` SET isUnread = 0 WHERE id = ?", TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      final int rowsAffected = ps.executeUpdate();
      LOG.debug("Make event read with id: {}. Rows affected: {}.", id, rowsAffected);
      return true;
    } catch (SQLException ex) {
      LOG.error("Unable to make event with id: {} read. Cause: {}", id, ex.getMessage());
    }
    return false;
  }

  // execute with blocking mode on every new incoming event
  @Override
  public long persist(String eventSource, EmailProperties emailProperties) {
    final String sql = String.format(
      "INSERT INTO `%s` (eventSource, subject, rawBody, eventTime, isUnread) VALUES (?,?,?,?,?)",
      TABLE_NAME
    );
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventSource);
      ps.setString(2, emailProperties.subject());
      ps.setString(3, emailProperties.rawBody());
      ps.setTimestamp(4, Timestamp.valueOf(emailProperties.eventTime()));
      ps.setBoolean(5, true);
      final int rowsAffected = ps.executeUpdate();
      if (rowsAffected == 0) {
        return -1;
      }
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (!rs.next()) {
          return -1;
        }
        final long generatedId = rs.getLong(1);
        LOG.debug("Persist event from event source: {}. Rows affected: {}. Event: {}",
          eventSource, rowsAffected, emailProperties);
        return generatedId;
      }
    } catch (SQLException ex) {
      LOG.error("Unable to persist event from event source: {}. Cause: {}", eventSource,
        ex.getMessage());
    }
    return -1;
  }

  @Override
  public boolean deleteAllByEventSource(String eventSource) {
    final String sql = String.format("DELETE FROM `%s` WHERE eventSource = ?;", TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, eventSource);
      final int affectedRows = ps.executeUpdate(sql);
      if (affectedRows > 0) {
        LOG.warn("Deleted all rows ({}) from event source: {}", affectedRows, eventSource);
      }
      return true;
    } catch (SQLException ex) {
      LOG.error("Unable to delete all from event source: {}. Cause: {}", eventSource,
        ex.getMessage());
    }
    return false;
  }

  @Override
  public boolean deleteSingleById(long id) {
    final String sql = String.format("DELETE FROM `%s` WHERE id = ?;", TABLE_NAME);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      final int affectedRows = ps.executeUpdate();
      if (affectedRows > 0) {
        LOG.info("Deleted row with id: {}", id);
      }
      return true;
    } catch (SQLException ex) {
      LOG.error("Unable to delete record by id: {}. Cause: {}", id, ex.getMessage());
    }
    return false;
  }
}
