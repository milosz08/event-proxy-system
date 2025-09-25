package pl.miloszgilga.event.proxy.server.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface StatementConsumer {
  void accept(PreparedStatement statement, int index, Object value) throws SQLException;
}
