package pl.miloszgilga.event.proxy.server;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class EventProxyServerMain implements Runnable {
  private final EventBroadcaster eventBroadcaster;
  private final HttpProxyServerThread httpProxyServerThread;
  private final SmtpProxyServerThread smtpProxyServerThread;

  private final EmailConsumer emailConsumer;

  EventProxyServerMain() {
    final ContentInitializerRegistry initializerRegistry = new ContentInitializerRegistry();
    final BlockingQueue<EmailContent> queue = new ArrayBlockingQueue<>(10);

    eventBroadcaster = new EventBroadcaster();
    httpProxyServerThread = new HttpProxyServerThread(4365, eventBroadcaster);
    smtpProxyServerThread = new SmtpProxyServerThread(4366, 10, queue);

    // init database
    final DbConnectionPool dbConnectionPool = DbConnectionPool.getInstance("events.db", 5);

    // register here new email parsers
    final List<EmailParser> emailParsers = List.of(
      new DvrEmailParser(),
      new NasEmailParser()
    );

    final EmailDao emailDao = new JdbcEmailDao(dbConnectionPool, emailParsers);

    initializerRegistry.register(emailDao);

    initializerRegistry.init();
    emailConsumer = new EmailConsumer(queue, emailParsers, eventBroadcaster, emailDao);
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
    eventBroadcaster.close();
    httpProxyServerThread.stop();
    smtpProxyServerThread.stop();
    emailConsumer.stop();
  }
}
