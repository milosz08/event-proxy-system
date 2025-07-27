package pl.miloszgilga.event.proxy.server;

import java.sql.JDBCType;

public record EmailPropertyValue(Object value, JDBCType type) {
  public EmailPropertyValue(Object value) {
    this(value, JDBCType.VARCHAR);
  }
}
