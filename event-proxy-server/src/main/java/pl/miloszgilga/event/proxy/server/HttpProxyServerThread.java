package pl.miloszgilga.event.proxy.server;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpProxyServerThread extends AbstractThread {
  private static final Logger LOG = LoggerFactory.getLogger(HttpProxyServerThread.class);

  private final int port;

  HttpProxyServerThread(int port) {
    super("HTTP-Proxy");
    this.port = port;
  }

  @Override
  public void run() {
    final Server server = new Server(port);
    final ServletContextHandler context = new ServletContextHandler();

    context.setContextPath("/");
    server.setHandler(context);

    try {
      server.start();
      server.join();
    } catch (Exception ex) {
      LOG.error("Unable to start HTTP proxy server. Cause: {}.", ex.getMessage());
    }
  }
}
