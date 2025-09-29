package pl.miloszgilga.event.proxy.server.parser;

import pl.miloszgilga.event.proxy.server.db.StatementConsumer;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public enum FieldType {
  TEXT((ps, i, value) -> ps.setString(i, (String) value)),
  INTEGER((ps, i, value) -> ps.setInt(i, (Integer) value)),
  REAL((ps, i, value) -> ps.setDouble(i, (Double) value)),
  NUMERIC((ps, i, value) -> ps.setBigDecimal(i, (BigDecimal) value)),
  TIMESTAMP((ps, i, value) -> ps.setTimestamp(i, (Timestamp) value)),
  ;

  private final StatementConsumer statementConsumer;

  FieldType(StatementConsumer statementConsumer) {
    this.statementConsumer = statementConsumer;
  }

  // fields must be mapped to specialized methods, because SQLite driver not implements setObject
  public void executeStatement(PreparedStatement ps, int i, Object value) throws SQLException {
    statementConsumer.accept(ps, i, value);
  }
}
