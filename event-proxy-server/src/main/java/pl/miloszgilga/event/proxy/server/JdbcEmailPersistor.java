package pl.miloszgilga.event.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class JdbcEmailPersistor implements EmailPersistor {
  private static final Logger LOG = LoggerFactory.getLogger(JdbcEmailPersistor.class);

  private final DbConnectionPool dbConnectionPool;
  private final List<EmailParser> emailParsers;

  JdbcEmailPersistor(DbConnectionPool dbConnectionPool, List<EmailParser> emailParsers) {
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
        sql.append(String.format("%s %s, ", field.getKey(), field.getValue().getSqliteType()));
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

  // execute with blocking mode on every new incoming event
  @Override
  public void persist(String dataName, List<EmailPropertyValue> emailData) {
    final String sql = String.format(
      "INSERT INTO `%s` (%s) VALUES (%s)",
      dataName,
      String.join(",", emailData.stream().map(EmailPropertyValue::name).toList()),
      String.join(",", Collections.nCopies(emailData.size(), "?"))
    );
    try (final Connection conn = dbConnectionPool.getConnection();
         final PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
      for (int i = 0; i < emailData.size(); i++) {
        final EmailPropertyValue emailPropertyValue = emailData.get(i);
        emailPropertyValue.fieldType().executeStatement(preparedStatement, i + 1,
          emailPropertyValue.value());
      }
      final int rowsAffected = preparedStatement.executeUpdate();
      LOG.debug("Persist event from: {}. Rows affected: {}. Event: {}", dataName, rowsAffected,
        emailData);
    } catch (SQLException ex) {
      LOG.error("Unable to persist event from: {}. Cause: {}", dataName, ex.getMessage());
    }
  }
}
