package pl.miloszgilga.event.proxy.server.http.sse;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SseServlet extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(SseServlet.class);

  private final EventBroadcaster eventBroadcaster;

  public SseServlet(EventBroadcaster eventBroadcaster) {
    this.eventBroadcaster = eventBroadcaster;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) {
    res.setContentType("text/event-stream");
    res.setHeader("Cache-Control", "no-cache");
    res.setHeader("Connection", "keep-alive");

    final String clientId = eventBroadcaster.addClientIdToRequest(req);

    final AsyncContext asyncContext = req.startAsync(req, res);
    asyncContext.setTimeout(0); // set no limit for SSE connections
    eventBroadcaster.addClient(clientId, asyncContext);

    asyncContext.addListener(new AsyncListener() {
      @Override
      public void onComplete(AsyncEvent event) {
        onDisconnectClient(asyncContext);
      }
      @Override
      public void onTimeout(AsyncEvent event) {
        onDisconnectClient(asyncContext);
      }
      @Override
      public void onError(AsyncEvent event) {
        onDisconnectClient(asyncContext);
      }
      @Override
      public void onStartAsync(AsyncEvent event) {
      }
    });
    LOG.info("Client {} connected. Active clients: {}", clientId,
      eventBroadcaster.getClientsCount());
  }

  private void onDisconnectClient(AsyncContext asyncContext) {
    final String clientId = eventBroadcaster.removeClient(asyncContext);
    LOG.info("Client {} disconnected. Active clients: {}", clientId,
      eventBroadcaster.getClientsCount());
  }
}
