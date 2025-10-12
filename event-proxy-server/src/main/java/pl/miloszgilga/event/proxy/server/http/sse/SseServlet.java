package pl.miloszgilga.event.proxy.server.http.sse;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
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
    final String sessionId = req.getParameter("sessionId");

    if (!eventBroadcaster.checkClient(sessionId)) {
      res.setStatus(HttpStatus.UNAUTHORIZED_401);
      return;
    }
    res.setContentType("text/event-stream");
    res.setHeader("Cache-Control", "no-cache");
    res.setHeader("Connection", "keep-alive");

    final AsyncContext asyncContext = req.startAsync(req, res);
    asyncContext.setTimeout(0); // set no limit for SSE connections

    eventBroadcaster.bindClient(sessionId, asyncContext, req);

    final CustomAsyncContextListener listener = new CustomAsyncContextListener(
      asyncContext,
      eventBroadcaster
    );
    asyncContext.addListener(listener);
    LOG.info("SSE client {} connected. Active clients: {}", sessionId,
      eventBroadcaster.getClientsCount());
  }
}
