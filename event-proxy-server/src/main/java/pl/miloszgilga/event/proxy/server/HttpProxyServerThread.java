package pl.miloszgilga.event.proxy.server;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpProxyServerThread extends AbstractThread {
  private static final Logger LOG = LoggerFactory.getLogger(HttpProxyServerThread.class);

  private final int port;

  private Server server;

  HttpProxyServerThread(int port) {
    super("HTTP-Proxy");
    this.port = port;
  }

  @Override
  public void run() {
    server = new Server(port);
    final ServletContextHandler context = new ServletContextHandler();

    context.setContextPath("/");
    server.setHandler(context);

    try {
      server.start();
      server.join();
    } catch (InterruptedException ignored) {
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
