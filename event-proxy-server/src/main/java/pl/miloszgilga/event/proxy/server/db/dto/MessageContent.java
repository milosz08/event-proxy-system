package pl.miloszgilga.event.proxy.server.db.dto;

import java.time.LocalDateTime;

public record MessageContent(Long id, String subject, LocalDateTime eventTime, Boolean isUnread) {
}
