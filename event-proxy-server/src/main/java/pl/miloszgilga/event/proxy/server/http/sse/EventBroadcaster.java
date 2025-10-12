package pl.miloszgilga.event.proxy.server.http.sse;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.Constants;
import pl.miloszgilga.event.proxy.server.crypto.AesEncryptedData;
import pl.miloszgilga.event.proxy.server.crypto.Crypto;
import pl.miloszgilga.event.proxy.server.parser.EmailPropertyValue;

import javax.crypto.SecretKey;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

public class EventBroadcaster implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(EventBroadcaster.class);

  private final long handshakeDestroyTime;
  private final Map<String, SecretKey> sseAesKeys;
  private final Map<String, AsyncContext> broadcastClients;
  private final Map<String, ScheduledFuture<?>> sessionDestroyers;

  private final ScheduledExecutorService sessionDestroyScheduler;
  private final ScheduledExecutorService heartbeatScheduler;

  public EventBroadcaster(long interval, long handshakeDestroyTime) {
    this.handshakeDestroyTime = handshakeDestroyTime;
    sseAesKeys = new ConcurrentHashMap<>();
    broadcastClients = new ConcurrentHashMap<>();
    sessionDestroyers = new ConcurrentHashMap<>();
    sessionDestroyScheduler = Executors.newSingleThreadScheduledExecutor();
    heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, interval, interval,
      TimeUnit.SECONDS);
  }

  String createSessionClient(SecretKey aesKey) {
    final String uuid = UUID.randomUUID().toString();
    sseAesKeys.put(uuid, aesKey);
    final ScheduledFuture<?> cleanupTask = sessionDestroyScheduler.schedule(() -> {
      sseAesKeys.remove(uuid);
      LOG.info("Remove orphan SSE client with id: {}", uuid);
    }, handshakeDestroyTime, TimeUnit.SECONDS);
    sessionDestroyers.put(uuid, cleanupTask);
    return uuid;
  }

  boolean checkClient(String sessionId) {
    return sseAesKeys.containsKey(sessionId);
  }

  void bindClient(String sessionId, AsyncContext asyncContext, HttpServletRequest req) {
    req.setAttribute(Constants.SSE_SESSION_ID_LABEL, sessionId);
    final ScheduledFuture<?> cleanupTask = sessionDestroyers.get(sessionId); // NEVER will be null!
    cleanupTask.cancel(false);
    broadcastClients.put(sessionId, asyncContext);
  }

  void removeSession(String sessionId) {
    sseAesKeys.remove(sessionId);
    broadcastClients.remove(sessionId);
  }

  int getClientsCount() {
    return broadcastClients.size();
  }

  public void broadcastEvent(String eventSource, List<EmailPropertyValue> emailProperties) {
    final JSONObject root = new JSONObject();
    final JSONArray dataFields = new JSONArray();
    for (final EmailPropertyValue eventProperty : emailProperties) {
      final JSONObject property = new JSONObject();
      property.put("name", eventProperty.name());
      property.put("value", eventProperty.value());
      property.put("type", eventProperty.fieldType().name());
      dataFields.put(property);
    }
    root.put("eventSource", eventSource);
    root.put("dataFields", dataFields);

    final String rawJsonData = root.toString();
    final Set<String> clients = broadcastClients.keySet();

    // parallel stream for split jobs to many threads
    clients.parallelStream().forEach(clientId -> {
      final AsyncContext asyncContext = broadcastClients.get(clientId);
      final SecretKey aesKey = sseAesKeys.get(clientId);
      try {
        final AesEncryptedData data = Crypto.encryptDataAes(rawJsonData, aesKey);
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("iv", data.iv());
        jsonObject.put("encrypted", data.base64data());
        sendToChannel(asyncContext, "data", jsonObject.toString());
      } catch (Exception ignored) {
      }
    });
  }

  // send heartbeat to check, if connection is still active, otherwise disconnect client
  private void sendHeartbeat() {
    final Collection<AsyncContext> asyncContexts = broadcastClients.values();
    asyncContexts.parallelStream()
      .forEach(asyncContext -> sendToChannel(asyncContext, "", "heartbeat"));
  }

  private void sendToChannel(AsyncContext context, String heading, String data) {
    boolean isError = false;
    try {
      final PrintWriter writer = context.getResponse().getWriter();
      writer.write(String.format("%s: %s\n\n", heading, data));
      writer.flush();
      if (writer.checkError()) {
        isError = true;
      }
    } catch (IOException ignored) {
      isError = true;
    }
    if (isError) {
      context.complete();
    }
  }

  @Override
  public void close() {
    heartbeatScheduler.shutdown();
  }
}
