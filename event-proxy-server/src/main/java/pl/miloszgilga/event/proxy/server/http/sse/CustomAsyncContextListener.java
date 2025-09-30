package pl.miloszgilga.event.proxy.server.http.sse;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CustomAsyncContextListener implements AsyncListener {
  private static final Logger LOG = LoggerFactory.getLogger(CustomAsyncContextListener.class);

  private final AsyncContext asyncContext;
  private final EventBroadcaster eventBroadcaster;

  CustomAsyncContextListener(AsyncContext asyncContext, EventBroadcaster eventBroadcaster) {
    this.asyncContext = asyncContext;
    this.eventBroadcaster = eventBroadcaster;
  }

  @Override
  public void onComplete(AsyncEvent asyncEvent) {
    onDisconnectClient();
  }

  @Override
  public void onTimeout(AsyncEvent asyncEvent) {
    onDisconnectClient();
  }

  @Override
  public void onError(AsyncEvent asyncEvent) {
    onDisconnectClient();
  }

  @Override
  public void onStartAsync(AsyncEvent asyncEvent) {
  }

  private void onDisconnectClient() {
    final String clientId = eventBroadcaster.removeClient(asyncContext);
    LOG.info("Client {} disconnected. Active clients: {}", clientId,
      eventBroadcaster.getClientsCount());
  }
}
