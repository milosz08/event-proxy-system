package pl.miloszgilga.event.proxy.server.http.sse;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.parser.EmailPropertyValue;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventBroadcaster implements Closeable {
  private static final String SSE_CLIENT_ID_HEADER = "sseClientId";

  private final Map<String, AsyncContext> broadcastClients;
  private final ScheduledExecutorService heartbeatScheduler;

  public EventBroadcaster(long interval) {
    broadcastClients = new ConcurrentHashMap<>();
    heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, interval, interval,
      TimeUnit.SECONDS);
  }

  String addClientIdToRequest(HttpServletRequest req) {
    final String uuid = UUID.randomUUID().toString();
    req.setAttribute(SSE_CLIENT_ID_HEADER, uuid);
    return uuid;
  }

  void addClient(String clientId, AsyncContext asyncContext) {
    broadcastClients.put(clientId, asyncContext);
  }

  String removeClient(AsyncContext asyncContext) {
    final String clientId = asyncContext.getRequest().getAttribute(SSE_CLIENT_ID_HEADER).toString();
    broadcastClients.remove(clientId);
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

    final String jsonData = root.toString();
    for (final AsyncContext clientContext : broadcastClients.values()) {
      sendToChannel(clientContext, "data", jsonData);
    }
  }

  // send heartbeat to check, if connection is still active, otherwise disconnect client
  private void sendHeartbeat() {
    for (final AsyncContext clientContext : broadcastClients.values()) {
      sendToChannel(clientContext, "", "heartbeat");
    }
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
