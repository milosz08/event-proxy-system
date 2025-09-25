package pl.miloszgilga.event.proxy.server.http;

import java.util.List;

public record Page<T>(List<T> elements, boolean hasNext, long totalElements) {
}
