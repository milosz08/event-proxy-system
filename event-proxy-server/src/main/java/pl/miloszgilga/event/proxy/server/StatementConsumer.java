package pl.miloszgilga.event.proxy.server;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
interface StatementConsumer {
  void accept(PreparedStatement statement, int index, Object value) throws SQLException;
}
