package pl.miloszgilga.event.proxy.server.parser;

public record EmailContent(String from, String subject, String rawBody) {
}
