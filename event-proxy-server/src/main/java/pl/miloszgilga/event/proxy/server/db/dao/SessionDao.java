package pl.miloszgilga.event.proxy.server.db.dao;

import pl.miloszgilga.event.proxy.server.http.SessionData;
import pl.miloszgilga.event.proxy.server.registry.ContentInitializer;

import java.time.Instant;

public interface SessionDao extends ContentInitializer {
  void createSession(String sessionId, Integer userId, String clientId, String publicKey,
                     Instant expiresAt);

  SessionData getSession(String sessionId);

  void updateSessionTime(String sessionId, Instant newExpiresAt);

  void destroySession(String sessionId);

  void removeExpired();
}
