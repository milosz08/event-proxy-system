package pl.miloszgilga.event.proxy.server.db.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.db.DbConnectionPool;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.http.Page;
import pl.miloszgilga.event.proxy.server.parser.EmailParser;
import pl.miloszgilga.event.proxy.server.parser.EmailPropertyValue;
import pl.miloszgilga.event.proxy.server.parser.FieldType;

import java.sql.*;
import java.util.*;

public class JdbcEventDao implements EventDao {
  private static final Logger LOG = LoggerFactory.getLogger(JdbcEventDao.class);

  private final DbConnectionPool dbConnectionPool;
  private final List<EmailParser> emailParsers;

  public JdbcEventDao(DbConnectionPool dbConnectionPool, List<EmailParser> emailParsers) {
    this.dbConnectionPool = dbConnectionPool;
    this.emailParsers = emailParsers;
  }

  @Override
  public void init() {
    // generate tables on app init for every email parsers (if this tables does not exist yet)
    for (final EmailParser emailParser : emailParsers) {
      final String tableName = emailParser.parserName();
      final Map<String, FieldType> parserFields = emailParser.declareParserFields();

      final StringBuilder sql = new StringBuilder();
      sql.append(String.format("CREATE TABLE IF NOT EXISTS `%s` ( ", tableName));

      sql.append("id INTEGER PRIMARY KEY AUTOINCREMENT, ");
      for (final Map.Entry<String, FieldType> field : parserFields.entrySet()) {
        sql.append(String.format("%s %s, ", field.getKey(), field.getValue().name()));
      }
      sql.setLength(sql.length() - 2);
      sql.append(");");

      try (final Connection conn = dbConnectionPool.getConnection();
           final Statement statement = conn.createStatement()) {
        statement.execute(sql.toString());
        LOG.info("Init table (or skip): {} for: {}", tableName, emailParser.getClass().getName());
      } catch (SQLException ex) {
        LOG.error("Unable to create table: {}. Cause: {}", tableName, ex.getMessage());
      }
    }
  }

  @Override
  public Page<Map<String, Object>> getAllByEventSource(String eventSource, int limit, int offset) {
    final List<Map<String, Object>> results = new ArrayList<>();
    long totalElements = 0;

    final String countSql = String.format("SELECT COUNT(*) FROM `%s`;", eventSource);
    final String sql = String.format("""
        SELECT id, subject, eventTime FROM `%s` ORDER BY id DESC LIMIT ? OFFSET ?;
      """, eventSource);

    try (final Connection conn = dbConnectionPool.getConnection()) {
      try (final PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, limit);
        ps.setInt(2, offset);
        try (final ResultSet rs = ps.executeQuery()) {
          final ResultSetMetaData metaData = rs.getMetaData();
          final int columnCount = metaData.getColumnCount();
          while (rs.next()) {
            // linked hashmap persist columns order
            final Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
              row.put(metaData.getColumnName(i), rs.getObject(i));
            }
            results.add(row);
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
      LOG.error("Unable to get all records from table: {}. Cause: {}", eventSource,
        ex.getMessage());
      return new Page<>(Collections.emptyList(), false, 0);
    }
    final boolean hasNext = (long) offset + results.size() < totalElements;
    return new Page<>(results, hasNext, totalElements);
  }

  @Override
  public Map<String, Object> getSingleById(String eventSource, long id) {
    final String sql = String.format("SELECT * FROM `%s` WHERE id = ?;", eventSource);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (final ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          final ResultSetMetaData metaData = rs.getMetaData();
          final int columnCount = metaData.getColumnCount();
          final Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= columnCount; i++) {
            row.put(metaData.getColumnName(i), rs.getObject(i));
          }
          return row;
        }
      }
    } catch (SQLException ex) {
      LOG.error("Unable to get record by id from table: {}. Cause: {}", eventSource,
        ex.getMessage());
    }
    return null;
  }

  // execute with blocking mode on every new incoming event
  @Override
  public void persist(String eventSource, List<EmailPropertyValue> emailProperties) {
    final String sql = String.format(
      "INSERT INTO `%s` (%s) VALUES (%s)",
      eventSource,
      String.join(",", emailProperties.stream().map(EmailPropertyValue::name).toList()),
      String.join(",", Collections.nCopies(emailProperties.size(), "?"))
    );
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
      for (int i = 0; i < emailProperties.size(); i++) {
        final EmailPropertyValue emailPropertyValue = emailProperties.get(i);
        emailPropertyValue.fieldType().executeStatement(preparedStatement, i + 1,
          emailPropertyValue.value());
      }
      final int rowsAffected = preparedStatement.executeUpdate();
      LOG.debug("Persist event from: {}. Rows affected: {}. Event: {}", eventSource, rowsAffected,
        emailProperties);
    } catch (SQLException ex) {
      LOG.error("Unable to persist event from: {}. Cause: {}", eventSource, ex.getMessage());
    }
  }

  @Override
  public void deleteAllByEventSource(String eventSource) {
    final String sql = String.format("DELETE FROM `%s`;", eventSource);
    try (final Connection conn = dbConnectionPool.getConnection();
         final Statement statement = conn.createStatement()) {
      final int affectedRows = statement.executeUpdate(sql);
      LOG.warn("Deleted all rows ({}) from table: {}", affectedRows, eventSource);
    } catch (SQLException ex) {
      LOG.error("Unable to delete all from table: {}. Cause: {}", eventSource, ex.getMessage());
    }
  }

  @Override
  public void deleteSingleById(String eventSource, long id) {
    final String sql = String.format("DELETE FROM `%s` WHERE id = ?;", eventSource);
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, id);
      final int affectedRows = ps.executeUpdate();
      if (affectedRows > 0) {
        LOG.info("Deleted row with id: {} from table: {}", id, eventSource);
      }
    } catch (SQLException ex) {
      LOG.error("Unable to delete record by id from table: {}. Cause: {}", eventSource,
        ex.getMessage());
    }
  }
}
