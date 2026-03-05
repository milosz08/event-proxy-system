package pl.miloszgilga.event.proxy.server.http;

public enum EventTableSource {
  EVENTS("events"),
  EVENTS_ARCHIVE("events_archive"),
  ;

  private final String tableName;

  EventTableSource(String tableName) {
    this.tableName = tableName;
  }

  public static EventTableSource fromString(String value) {
    if (value == null) return null;
    for (final EventTableSource type : values()) {
      if (type.name().equalsIgnoreCase(value)) {
        return type;
      }
    }
    return null;
  }

  public String getTableName() {
    return tableName;
  }
}
