package pl.miloszgilga.event.proxy.server;

import java.time.Duration;
import java.time.Instant;

interface SessionDao extends ContentInitializer {
  String createSession(String clientId, String publicKey, Duration sessionTime);

  Instant updateSessionTime(String sessionId, Duration sessionTime);

  void destroySession(String sessionId);
}
