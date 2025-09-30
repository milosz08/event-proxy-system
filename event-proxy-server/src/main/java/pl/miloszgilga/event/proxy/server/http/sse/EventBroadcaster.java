package pl.miloszgilga.event.proxy.server.http.sse;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.Constants;
import pl.miloszgilga.event.proxy.server.crypto.AesEncryptedData;
import pl.miloszgilga.event.proxy.server.crypto.Crypto;
import pl.miloszgilga.event.proxy.server.parser.EmailPropertyValue;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventBroadcaster implements Closeable {
  private final Map<String, AsyncContext> broadcastClients;
  private final Map<String, HybridKey> broadcastClientKeys;
  private final ScheduledExecutorService heartbeatScheduler;

  public EventBroadcaster(long interval) {
    broadcastClients = new ConcurrentHashMap<>();
    broadcastClientKeys = new ConcurrentHashMap<>();
    heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, interval, interval,
      TimeUnit.SECONDS);
  }

  String addClientIdToRequest(HttpServletRequest req) {
    final String uuid = UUID.randomUUID().toString();
    req.setAttribute(Constants.SSE_CLIENT_ID_LABEL, uuid);
    return uuid;
  }

  void addClient(String clientId, AsyncContext asyncContext, HybridKey hybridKey) {
    broadcastClients.put(clientId, asyncContext);
    broadcastClientKeys.put(clientId, hybridKey);
  }

  String removeClient(AsyncContext asyncContext) {
    final String clientId = asyncContext.getRequest().getAttribute(Constants.SSE_CLIENT_ID_LABEL)
      .toString();
    broadcastClients.remove(clientId);
    broadcastClientKeys.remove(clientId);
    return clientId;
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
      final HybridKey hybridKey = broadcastClientKeys.get(clientId);
      try {
        final AesEncryptedData data = Crypto.encryptDataAes(rawJsonData, hybridKey.aes());
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("iv", data.iv());
        jsonObject.put("aes", hybridKey.encryptedAes());
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
