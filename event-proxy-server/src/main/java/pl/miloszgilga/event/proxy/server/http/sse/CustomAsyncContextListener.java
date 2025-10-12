package pl.miloszgilga.event.proxy.server.http.sse;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.Constants;

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
    final String sessionId = asyncContext.getRequest().getAttribute(Constants.SSE_SESSION_ID_LABEL)
      .toString();
    eventBroadcaster.removeSession(sessionId);
    LOG.info("SSE client {} disconnected. Active clients: {}", sessionId,
      eventBroadcaster.getClientsCount());
  }
}
