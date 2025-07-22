package pl.miloszgilga.event.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SmtpProxyServerThread extends AbstractThread {
  private final Logger LOG = LoggerFactory.getLogger(SmtpProxyServerThread.class);

  private final int port;
  private final int threadPoolSize;
  private final ExecutorService threadPool;
  private final BlockingQueue<EmailContent> queue;

  private ServerSocket serverSocket;

  SmtpProxyServerThread(int port, int threadPoolSize, BlockingQueue<EmailContent> queue) {
    super("SMTP-Proxy");
    this.port = port;
    this.threadPoolSize = threadPoolSize;
    this.queue = queue;
    threadPool = Executors.newFixedThreadPool(threadPoolSize);
  }

  @Override
  public void run() {
    try {
      serverSocket = new ServerSocket(port);
      LOG.info("Started SMTP proxy server at {} port in local network area", port);
      LOG.info("Initialized fixed thread pool with {} threads", threadPoolSize);
      while (running && !Thread.currentThread().isInterrupted()) {
        try {
          final Socket clientSocket = serverSocket.accept();
          threadPool.execute(new SmtpMessageReceiver(clientSocket, queue));
        } catch (IOException ex) {
          if (running) {
            LOG.error("Unable to connect with SMTP client. Cause: {}", ex.getMessage());
          }
        }
      }
    } catch (SocketException ignored) {
    } catch (IOException ex) {
      LOG.error("Unable to start SMTP proxy server. Cause: {}", ex.getMessage());
    } finally {
      if (serverSocket != null && !serverSocket.isClosed()) {
        try {
          serverSocket.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  @Override
  void beforeStopThread() {
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException ignored) {
    }
    threadPool.shutdown();
    LOG.info("SMTP proxy server has been stopped");
  }
}
