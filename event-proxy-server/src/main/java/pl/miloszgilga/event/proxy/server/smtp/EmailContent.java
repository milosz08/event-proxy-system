package pl.miloszgilga.event.proxy.server.smtp;

import java.time.Instant;

public record EmailContent(String from, String subject, String rawBody, Instant receivedAt) {
}
