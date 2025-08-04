package pl.miloszgilga.event.proxy.server;

import jakarta.servlet.AsyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class EventBroadcaster implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(EventBroadcaster.class);

  private final List<AsyncContext> broadcastClients;
  private final ScheduledExecutorService heartbeatScheduler;

  EventBroadcaster() {
    broadcastClients = new CopyOnWriteArrayList<>();
    heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, 10, 10, TimeUnit.SECONDS);
  }

  void addClient(AsyncContext asyncContext) {
    broadcastClients.add(asyncContext);
  }

  void removeClient(AsyncContext asyncContext) {
    broadcastClients.remove(asyncContext);
  }

  int getClientsCount() {
    return broadcastClients.size();
  }

  void broadcastEvent(String dataName, List<EmailPropertyValue> eventData) {
    for (final AsyncContext clientContext : broadcastClients) {
      try {
        // TODO: change fields into JSON or param map (separated by =)
        sendToChannel(clientContext, "data", eventData.toString());
      } catch (IOException ex) {
        LOG.error("Unable to send event from: {}. Cause: {}", dataName, ex.getMessage());
      }
    }
  }

  // send heartbeat to check, if connection is still active, otherwise disconnect client
  private void sendHeartbeat() {
    for (final AsyncContext clientContext : broadcastClients) {
      try {
        sendToChannel(clientContext, "", "heartbeat");
      } catch (IOException ignored) {
        clientContext.complete();
      }
    }
  }

  private void sendToChannel(AsyncContext context, String heading, String data) throws IOException {
    final PrintWriter writer = context.getResponse().getWriter();
    writer.write(String.format("%s: %s\n\n", heading, data));
    writer.flush();
  }

  @Override
  public void close() {
    heartbeatScheduler.shutdown();
  }
}
