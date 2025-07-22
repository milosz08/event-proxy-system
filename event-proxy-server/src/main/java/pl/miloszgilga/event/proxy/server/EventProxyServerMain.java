package pl.miloszgilga.event.proxy.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class EventProxyServerMain implements Runnable {
  private final HttpProxyServerThread httpProxyServerThread;
  private final SmtpProxyServerThread smtpProxyServerThread;
  private final EmailConsumer emailConsumer;

  EventProxyServerMain() {
    final BlockingQueue<EmailContent> queue = new ArrayBlockingQueue<>(10);

    httpProxyServerThread = new HttpProxyServerThread(4365);
    smtpProxyServerThread = new SmtpProxyServerThread(1025, 10, queue);
    emailConsumer = new EmailConsumer(queue);
  }

  public static void main(String[] args) {
    final EventProxyServerMain main = new EventProxyServerMain();
    Runtime.getRuntime().addShutdownHook(new Thread(main));
    main.start();
  }

  void start() {
    httpProxyServerThread.start();
    smtpProxyServerThread.start();
    emailConsumer.start();
  }

  @Override
  public void run() {
    httpProxyServerThread.stop();
    smtpProxyServerThread.stop();
    emailConsumer.stop();
  }
}
