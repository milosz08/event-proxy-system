package pl.miloszgilga.event.proxy.server.queue;

import java.time.Instant;

public record EmailProperties(String subject, String rawBody, Instant eventTime) {
}
