package pl.miloszgilga.event.proxy.server.db.dao;

import pl.miloszgilga.event.proxy.server.registry.ContentInitializer;

import java.time.Duration;
import java.time.Instant;

public interface SessionDao extends ContentInitializer {
  String createSession(String clientId, String publicKey, Duration sessionTime);

  Instant updateSessionTime(String sessionId, Duration sessionTime);

  void destroySession(String sessionId);
}
