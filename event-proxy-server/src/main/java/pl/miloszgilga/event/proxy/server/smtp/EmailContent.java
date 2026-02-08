package pl.miloszgilga.event.proxy.server.smtp;

import java.time.LocalDateTime;

public record EmailContent(String from, String subject, String rawBody, LocalDateTime receivedAt) {
}
