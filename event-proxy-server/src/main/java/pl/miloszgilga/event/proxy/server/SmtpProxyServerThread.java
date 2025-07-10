package pl.miloszgilga.event.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SmtpProxyServerThread extends Thread {
  private final Logger LOG = LoggerFactory.getLogger(SmtpProxyServerThread.class);

  private final int port;
  private final int threadPoolSize;
  private final ExecutorService threadPool;

  SmtpProxyServerThread(int port, int threadPoolSize) {
    super("SMTP-Proxy");
    this.port = port;
    this.threadPoolSize = threadPoolSize;
    threadPool = Executors.newFixedThreadPool(threadPoolSize);
  }

  @Override
  public void run() {
    try (final ServerSocket serverSocket = new ServerSocket(port)) {
      LOG.info("Started SMTP proxy server at {} port in local network area", port);
      LOG.info("Initialized fixed thread pool with {} threads", threadPoolSize);
      while (!serverSocket.isClosed()) {
        final Socket clientSocket = serverSocket.accept();
        threadPool.execute(new SmtpMessageReceiver(clientSocket));
      }
    } catch (SocketException ignored) {
    } catch (IOException ex) {
      LOG.error("Unable to start SMTP proxy server. Cause: {}", ex.getMessage());
    } finally {
      threadPool.shutdown();
    }
  }
}
