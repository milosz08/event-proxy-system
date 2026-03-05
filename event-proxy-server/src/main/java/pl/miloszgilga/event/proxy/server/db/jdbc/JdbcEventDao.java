package pl.miloszgilga.event.proxy.server.db.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.db.DbConnectionPool;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.db.dto.EventContent;
import pl.miloszgilga.event.proxy.server.db.dto.EventContentWithBody;
import pl.miloszgilga.event.proxy.server.http.EventTableSource;
import pl.miloszgilga.event.proxy.server.http.Page;
import pl.miloszgilga.event.proxy.server.queue.EmailProperties;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JdbcEventDao implements EventDao {
  private static final Logger LOG = LoggerFactory.getLogger(JdbcEventDao.class);

  private final DbConnectionPool dbConnectionPool;

  public JdbcEventDao(DbConnectionPool dbConnectionPool) {
    this.dbConnectionPool = dbConnectionPool;
  }

  @Override
  public void init() {
    for (final EventTableSource table : EventTableSource.values()) {
      final String tableName = table.getTableName();
      final String sql = String.format("""
          CREATE TABLE IF NOT EXISTS `%s` (
            id INTEGER PRIMARY KEY,
            eventSource TEXT NOT NULL,
            subject TEXT NOT NULL,
            rawBody TEXT NOT NULL,
            eventTime TIMESTAMP NOT NULL,
            isUnread INTEGER DEFAULT 0 CHECK (isUnread IN (0, 1))
          );
        """, tableName);
      try (final Connection conn = dbConnectionPool.getConnection();
           final Statement statement = conn.createStatement()) {
        statement.execute(sql);
        LOG.info("Init table (or skip): {}", tableName);
      } catch (SQLException ex) {
        LOG.error("Unable to create table: {}. Cause: {}", tableName, ex.getMessage());
      }
    }
  }

  @Override
  public Page<EventContent> getAllByOptionalEventSource(
    EventTableSource tableSource,
    String eventSource,
    int limit,
    int offset
  ) {
    final String tableName = tableSource.getTableName();
    final List<EventContent> results = new ArrayList<>();
    long totalElements = 0;

    final String countSql = String.format("SELECT COUNT(*) FROM `%s`;", tableName);
    final String sql = String.format("""
        SELECT id, subject, eventTime, eventSource, isUnread FROM `%s` %s
        ORDER BY id DESC LIMIT ? OFFSET ?;
      """, tableName, eventSource != null ? "WHERE eventSource = ?" : "");

    try (final Connection conn = dbConnectionPool.getConnection()) {
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        int columnIndex = 1;
        if (eventSource != null) {
          ps.setString(columnIndex++, eventSource);
        }
        ps.setInt(columnIndex++, limit);
        ps.setInt(columnIndex, offset);
        try (final ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            final EventContent eventContent = new EventContent(
              rs.getLong("id"),
              rs.getString("subject"),
              rs.getTimestamp("eventTime").toLocalDateTime(),
              rs.getString("eventSource"),
              rs.getBoolean("isUnread")
            );
            results.add(eventContent);
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
      LOG.error("Unable to get all records from event source: {} ({}). Cause: {}", eventSource,
        tableName, ex.getMessage());
      return new Page<>(Collections.emptyList(), false, 0);
    }
    final boolean hasNext = (long) offset + results.size() < totalElements;
    return new Page<>(results, hasNext, totalElements);
  }

  @Override
  public EventContentWithBody getSingleById(EventTableSource tableSource, long id) {
    final String tableName = tableSource.getTableName();
    final String sql = String.format("SELECT * FROM `%s` WHERE id = ?;", tableName);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (final ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new EventContentWithBody(
            rs.getLong("id"),
            rs.getString("subject"),
            rs.getString("rawBody"),
            rs.getTimestamp("eventTime").toLocalDateTime(),
            rs.getString("eventSource"),
            rs.getBoolean("isUnread")
          );
        }
      }
    } catch (SQLException ex) {
      LOG.error("Unable to get record by id: {} ({}). Cause: {}", id, tableName, ex.getMessage());
    }
    return null;
  }

  @Override
  public boolean eventSourceExists(String eventSource) {
    final EventTableSource[] sources = EventTableSource.values();
    final String queryFragments = Arrays.stream(sources)
      .map(table -> String.format("SELECT 1 FROM `%s` WHERE eventSource = ?", table.getTableName()))
      .collect(Collectors.joining(" UNION ALL "));
    final String sql = String.format("SELECT EXISTS (%s);", queryFragments);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      for (int i = 0; i < sources.length; i++) {
        ps.setString(i + 1, eventSource);
      }
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
  public boolean makeEventRead(EventTableSource tableSource, long id) {
    final String tableName = tableSource.getTableName();
    final String sql = String.format("UPDATE `%s` SET isUnread = 0 WHERE id = ?", tableName);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      final int rowsAffected = ps.executeUpdate();
      LOG.debug("Make event read with id: {} ({}). Rows affected: {}.", id, tableName,
        rowsAffected);
      return true;
    } catch (SQLException ex) {
      LOG.error("Unable to make event with id: {} read ({}). Cause: {}", id, tableName,
        ex.getMessage());
    }
    return false;
  }

  // execute with blocking mode on every new incoming event
  @Override
  public long persist(String eventSource, EmailProperties emailProperties) {
    final String sql = String.format(
      "INSERT INTO `%s` (eventSource, subject, rawBody, eventTime, isUnread) VALUES (?,?,?,?,?)",
      EventTableSource.EVENTS.getTableName()
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
  public boolean deleteAllByOptionalEventSource(EventTableSource tableSource, String eventSource) {
    final String tableName = tableSource.getTableName();
    final String sql = String.format(
      "DELETE FROM `%s` %s;",
      tableName, eventSource != null ? "WHERE eventSource = ?" : ""
    );
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      if (eventSource != null) {
        ps.setString(1, eventSource);
      }
      final int affectedRows = ps.executeUpdate();
      if (affectedRows > 0) {
        LOG.warn("Deleted all rows ({}) from event source: {} ({})", affectedRows, eventSource,
          tableName);
      }
      return true;
    } catch (SQLException ex) {
      LOG.error("Unable to delete all from event source: {} ({}). Cause: {}", eventSource,
        tableName, ex.getMessage());
    }
    return false;
  }

  @Override
  public boolean archiveAllByOptionalEventSource(String eventSource) {
    return moveAllBetweenTables(EventTableSource.EVENTS, EventTableSource.EVENTS_ARCHIVE,
      eventSource);
  }

  @Override
  public boolean archiveMultipleByIds(long[] ids) {
    return moveMultipleByIdsBetweenTables(EventTableSource.EVENTS, EventTableSource.EVENTS_ARCHIVE,
      ids);
  }

  @Override
  public boolean unarchiveAllByOptionalEventSource(String eventSource) {
    return moveAllBetweenTables(EventTableSource.EVENTS_ARCHIVE, EventTableSource.EVENTS,
      eventSource);
  }

  @Override
  public boolean unarchiveMultipleByIds(long[] ids) {
    return moveMultipleByIdsBetweenTables(EventTableSource.EVENTS_ARCHIVE, EventTableSource.EVENTS,
      ids);
  }

  @Override
  public boolean deleteMultipleByIds(EventTableSource tableSource, long[] ids) {
    final String tableName = tableSource.getTableName();
    if (ids == null || ids.length == 0) {
      return false;
    }
    final String sql = String.format("DELETE FROM `%s` WHERE id = ?;", tableName);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      conn.setAutoCommit(false);
      for (long id : ids) {
        ps.setLong(1, id);
        ps.addBatch();
      }
      final int[] affectedRows = ps.executeBatch();
      conn.commit();
      int totalDeleted = 0;
      for (int rows : affectedRows) {
        if (rows > 0) totalDeleted += rows;
      }
      LOG.info("Successfully deleted {} rows from {}", totalDeleted, tableName);
      return true;
    } catch (SQLException ex) {
      LOG.error("Unable to delete record by ids: {} ({}). Cause: {}", ids, tableName,
        ex.getMessage());
    }
    return false;
  }

  private boolean moveAllBetweenTables(
    EventTableSource source,
    EventTableSource target,
    String eventSource
  ) {
    final String sourceTable = source.getTableName();
    final String targetTable = target.getTableName();
    final String whereClause = eventSource != null ? "WHERE eventSource = ?" : "";

    final String insertSql = String.format(
      "INSERT INTO `%s` SELECT * FROM `%s` %s;",
      targetTable, sourceTable, whereClause
    );
    final String deleteSql = String.format("DELETE FROM `%s` %s;", sourceTable, whereClause);

    try (final Connection conn = dbConnectionPool.getConnection()) {
      conn.setAutoCommit(false);
      try (final PreparedStatement insertPs = conn.prepareStatement(insertSql);
           final PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
        if (eventSource != null) {
          insertPs.setString(1, eventSource);
          deletePs.setString(1, eventSource);
        }
        final int inserted = insertPs.executeUpdate();
        deletePs.executeUpdate();
        conn.commit();
        LOG.info("Moved {} rows from {} to {} (eventSource: {})", inserted, sourceTable,
          targetTable, eventSource);
        return true;
      } catch (SQLException ex) {
        conn.rollback();
        throw ex;
      }
    } catch (SQLException ex) {
      LOG.error("Unable to move records from {} to {}. Cause: {}", sourceTable, targetTable,
        ex.getMessage());
    }
    return false;
  }

  private boolean moveMultipleByIdsBetweenTables(
    EventTableSource source,
    EventTableSource target,
    long[] ids
  ) {
    if (ids == null || ids.length == 0) {
      return false;
    }
    final String sourceTable = source.getTableName();
    final String targetTable = target.getTableName();

    final String insertSql = String.format(
      "INSERT INTO `%s` SELECT * FROM `%s` WHERE id = ?;",
      targetTable, sourceTable
    );
    final String deleteSql = String.format("DELETE FROM `%s` WHERE id = ?;", sourceTable);

    try (final Connection conn = dbConnectionPool.getConnection()) {
      conn.setAutoCommit(false);
      try (final PreparedStatement insertPs = conn.prepareStatement(insertSql);
           final PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
        for (long id : ids) {
          insertPs.setLong(1, id);
          insertPs.addBatch();
          deletePs.setLong(1, id);
          deletePs.addBatch();
        }
        final int[] inserted = insertPs.executeBatch();
        deletePs.executeBatch();
        conn.commit();
        final int totalMoved = Arrays.stream(inserted).filter(i -> i > 0).sum();
        LOG.info("Moved {} rows by ids from {} to {}", totalMoved, sourceTable, targetTable);
        return true;
      } catch (SQLException ex) {
        conn.rollback();
        throw ex;
      }
    } catch (SQLException ex) {
      LOG.error("Unable to move records by ids from {} to {}. Cause: {}", sourceTable, targetTable,
        ex.getMessage());
    }
    return false;
  }
}
