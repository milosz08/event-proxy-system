package pl.miloszgilga.event.proxy.server;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

class EventBroadcaster implements Closeable {
  private static final String SSE_CLIENT_ID_HEADER = "sseClientId";

  private final Map<String, AsyncContext> broadcastClients;
  private final ScheduledExecutorService heartbeatScheduler;

  EventBroadcaster() {
    broadcastClients = new ConcurrentHashMap<>();
    heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, 10, 10, TimeUnit.SECONDS);
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

  void broadcastEvent(String dataName, EmailPropertiesAggregator emailProperties) {
    final String jsonData = emailProperties.serializeToJson(dataName);
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
