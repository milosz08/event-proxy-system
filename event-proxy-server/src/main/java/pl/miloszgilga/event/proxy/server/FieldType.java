package pl.miloszgilga.event.proxy.server;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

enum FieldType {
  TEXT("TEXT", (ps, i, value) -> ps.setString(i, (String) value)),
  INTEGER("INTEGER", (ps, i, value) -> ps.setInt(i, (Integer) value)),
  REAL("REAL", (ps, i, value) -> ps.setDouble(i, (Double) value)),
  NUMERIC("NUMERIC", (ps, i, value) -> ps.setBigDecimal(i, (BigDecimal) value)),
  ;

  private final String sqliteType;
  private final StatementConsumer statementConsumer;

  FieldType(String sqliteType, StatementConsumer statementConsumer) {
    this.sqliteType = sqliteType;
    this.statementConsumer = statementConsumer;
  }

  String getSqliteType() {
    return sqliteType;
  }

  // fields must be mapped to specialized methods, because SQLite driver not implements setObject
  void executeStatement(PreparedStatement ps, int i, Object value) throws SQLException {
    statementConsumer.accept(ps, i, value);
  }
}
