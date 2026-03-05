package pl.miloszgilga.event.proxy.server.db.dto;

import java.time.LocalDateTime;

public record EventContentWithBody(
  Long id,
  String subject,
  String rawBody,
  LocalDateTime eventTime,
  String eventSource,
  Boolean isUnread
) {
}
