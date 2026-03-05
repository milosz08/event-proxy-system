package pl.miloszgilga.event.proxy.server.db.dto;

import java.time.LocalDateTime;

public record EventContent(
  Long id,
  String subject,
  LocalDateTime eventTime,
  String eventSource,
  Boolean isUnread
) {
}
