package pl.miloszgilga.event.proxy.server.db.dto;

public record EventContent(
  Long id,
  String subject,
  Long eventTime,
  String eventSource,
  Boolean isUnread
) {
}
