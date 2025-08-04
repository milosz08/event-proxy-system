package pl.miloszgilga.event.proxy.server;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpProxyServerThread extends AbstractThread {
  private static final Logger LOG = LoggerFactory.getLogger(HttpProxyServerThread.class);

  private final int port;
  private final EventBroadcaster eventBroadcaster;

  private Server server;

  HttpProxyServerThread(int port, EventBroadcaster eventBroadcaster) {
    super("HTTP-Proxy");
    this.port = port;
    this.eventBroadcaster = eventBroadcaster;
  }

  @Override
  public void run() {
    server = new Server(port);
    final ServletContextHandler context = new ServletContextHandler();
    context.setContextPath("/");

    context.addServlet(new SseServlet(eventBroadcaster), "/events");

    server.setHandler(context);
    try {
      server.start();
      server.join();
    } catch (Exception ex) {
      LOG.error("Unable to start HTTP proxy server. Cause: {}", ex.getMessage());
    }
  }

  @Override
  void beforeStopThread() {
    if (server == null) {
      return;
    }
    try {
      server.stop();
    } catch (Exception ignored) {}
    LOG.info("HTTP proxy server has been stopped");
  }
}
