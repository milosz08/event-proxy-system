package pl.miloszgilga.event.proxy.server.db.dto;

public record EventContentWithBody(
  Long id,
  String subject,
  String rawBody,
  Long eventTime,
  String eventSource,
  Boolean isUnread
) {
}
