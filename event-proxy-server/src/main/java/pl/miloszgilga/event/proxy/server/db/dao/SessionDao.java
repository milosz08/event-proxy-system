package pl.miloszgilga.event.proxy.server.db.dao;

import pl.miloszgilga.event.proxy.server.registry.ContentInitializer;

import java.time.Duration;
import java.time.Instant;

public interface SessionDao extends ContentInitializer {
  void createSession(String sessionId, String clientId, String publicKey, String publicKeySha256,
                     Instant expiresAt);

  Instant updateSessionTime(String sessionId, Duration sessionTime);

  void destroySession(String sessionId);
}
