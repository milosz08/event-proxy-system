package pl.miloszgilga.event.proxy.server;

import pl.miloszgilga.event.proxy.server.db.DbConnectionPool;
import pl.miloszgilga.event.proxy.server.db.InstancePasswordManager;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;
import pl.miloszgilga.event.proxy.server.db.dao.UserDao;
import pl.miloszgilga.event.proxy.server.db.jdbc.JdbcEventDao;
import pl.miloszgilga.event.proxy.server.db.jdbc.JdbcSessionDao;
import pl.miloszgilga.event.proxy.server.db.jdbc.JdbcUserDao;
import pl.miloszgilga.event.proxy.server.http.ExpiredSessionRemoval;
import pl.miloszgilga.event.proxy.server.http.HttpProxyServerThread;
import pl.miloszgilga.event.proxy.server.http.sse.EventBroadcaster;
import pl.miloszgilga.event.proxy.server.queue.EmailConsumer;
import pl.miloszgilga.event.proxy.server.registry.ContentInitializerRegistry;
import pl.miloszgilga.event.proxy.server.smtp.EmailContent;
import pl.miloszgilga.event.proxy.server.smtp.SmtpProxyServerThread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class EventProxyServerMain implements Runnable {
  private final EventBroadcaster eventBroadcaster;
  private final HttpProxyServerThread httpProxyServerThread;
  private final SmtpProxyServerThread smtpProxyServerThread;

  private final EmailConsumer emailConsumer;
  private final ExpiredSessionRemoval expiredSessionRemoval;

  EventProxyServerMain() {
    final AppConfig appConfig = new AppConfig();

    final ContentInitializerRegistry initializerRegistry = new ContentInitializerRegistry();
    final BlockingQueue<EmailContent> queue =
      new ArrayBlockingQueue<>(appConfig.getAsInt(AppConfig.Prop.SMTP_QUEUE_CAPACITY));

    // init database
    final DbConnectionPool dbConnectionPool = DbConnectionPool.getInstance(
      appConfig.getAsStr(AppConfig.Prop.DB_PATH),
      appConfig.getAsInt(AppConfig.Prop.DB_POOL_SIZE)
    );

    final EventDao eventDao = new JdbcEventDao(dbConnectionPool);
    final UserDao userDao = new JdbcUserDao(dbConnectionPool);
    final SessionDao sessionDao = new JdbcSessionDao(dbConnectionPool);

    final InstancePasswordManager instancePasswordManager = new InstancePasswordManager(
      userDao,
      appConfig.getAsStr(AppConfig.Prop.ACCOUNT_USERNAME),
      appConfig.getAsInt(AppConfig.Prop.ACCOUNT_PASSWORD_LENGTH),
      appConfig.getAsInt(AppConfig.Prop.ACCOUNT_PASSWORD_HASH_STRENGTH)
    );
    expiredSessionRemoval = new ExpiredSessionRemoval(
      sessionDao,
      appConfig.getAsInt(AppConfig.Prop.SESSION_CLEAR_INTERVAL_SEC)
    );

    initializerRegistry.register(eventDao);
    initializerRegistry.register(userDao);
    initializerRegistry.register(sessionDao);
    initializerRegistry.register(instancePasswordManager);
    initializerRegistry.register(expiredSessionRemoval);

    eventBroadcaster = new EventBroadcaster(
      appConfig.getAsLong(AppConfig.Prop.SSE_HEARTBEAT_INTERVAL_SEC),
      appConfig.getAsLong(AppConfig.Prop.SSE_HANDSHAKE_PENDING_SEC)
    );
    httpProxyServerThread = new HttpProxyServerThread(
      appConfig.getAsInt(AppConfig.Prop.HTTP_PORT),
      eventBroadcaster,
      sessionDao,
      eventDao,
      userDao,
      appConfig,
      instancePasswordManager
    );
    smtpProxyServerThread = new SmtpProxyServerThread(
      appConfig.getAsInt(AppConfig.Prop.SMTP_PORT),
      appConfig.getAsInt(AppConfig.Prop.SMTP_THREAD_POOL_SIZE),
      queue
    );

    initializerRegistry.init();
    emailConsumer = new EmailConsumer(queue, eventBroadcaster, eventDao);
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
    expiredSessionRemoval.close();
  }
}
