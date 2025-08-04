package pl.miloszgilga.event.proxy.server;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class EventProxyServerMain implements Runnable {
  private final HttpProxyServerThread httpProxyServerThread;
  private final SmtpProxyServerThread smtpProxyServerThread;

  private final EmailConsumer emailConsumer;

  EventProxyServerMain() {
    final ContentInitializerRegistry initializerRegistry = new ContentInitializerRegistry();
    final BlockingQueue<EmailContent> queue = new ArrayBlockingQueue<>(10);

    httpProxyServerThread = new HttpProxyServerThread(4365);
    smtpProxyServerThread = new SmtpProxyServerThread(4366, 10, queue);

    // init database
    final DbConnectionPool dbConnectionPool = DbConnectionPool.getInstance("events.db", 5);

    // register here new email parsers
    final List<EmailParser> emailParsers = List.of(
      new DvrEmailParser(),
      new NasEmailParser()
    );

    final EmailPersistor emailPersistor = new JdbcEmailPersistor(dbConnectionPool, emailParsers);
    initializerRegistry.register(emailPersistor);

    initializerRegistry.init();
    emailConsumer = new EmailConsumer(queue, emailParsers, emailPersistor);
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
