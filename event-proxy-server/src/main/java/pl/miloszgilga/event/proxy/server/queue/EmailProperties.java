package pl.miloszgilga.event.proxy.server.queue;

import java.time.LocalDateTime;

public record EmailProperties(String subject, String rawBody, LocalDateTime eventTime) {
}
