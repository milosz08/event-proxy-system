package pl.miloszgilga.event.proxy.server;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

enum FieldType {
  TEXT((ps, i, value) -> ps.setString(i, (String) value)),
  INTEGER((ps, i, value) -> ps.setInt(i, (Integer) value)),
  REAL((ps, i, value) -> ps.setDouble(i, (Double) value)),
  NUMERIC((ps, i, value) -> ps.setBigDecimal(i, (BigDecimal) value)),
  ;

  private final StatementConsumer statementConsumer;

  FieldType(StatementConsumer statementConsumer) {
    this.statementConsumer = statementConsumer;
  }

  String getSqliteType() {
    return name();
  }

  // fields must be mapped to specialized methods, because SQLite driver not implements setObject
  void executeStatement(PreparedStatement ps, int i, Object value) throws SQLException {
    statementConsumer.accept(ps, i, value);
  }
}
