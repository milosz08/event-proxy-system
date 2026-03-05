package pl.miloszgilga.event.proxy.server.http;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.AbstractThread;
import pl.miloszgilga.event.proxy.server.AppConfig;
import pl.miloszgilga.event.proxy.server.db.InstancePasswordManager;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;
import pl.miloszgilga.event.proxy.server.db.dao.UserDao;
import pl.miloszgilga.event.proxy.server.http.filter.*;
import pl.miloszgilga.event.proxy.server.http.rest.*;
import pl.miloszgilga.event.proxy.server.http.sse.EventBroadcaster;
import pl.miloszgilga.event.proxy.server.http.sse.SseHandshakeServlet;
import pl.miloszgilga.event.proxy.server.http.sse.SseServlet;

import java.util.List;

public class HttpProxyServerThread extends AbstractThread {
  private static final Logger LOG = LoggerFactory.getLogger(HttpProxyServerThread.class);

  private final int port;
  private final EventBroadcaster eventBroadcaster;
  private final SessionDao sessionDao;
  private final EventDao eventDao;
  private final UserDao userDao;
  private final AppConfig appConfig;
  private final InstancePasswordManager instancePasswordManager;

  private Server server;

  public HttpProxyServerThread(int port, EventBroadcaster eventBroadcaster, SessionDao sessionDao,
                               EventDao eventDao, UserDao userDao, AppConfig appConfig,
                               InstancePasswordManager instancePasswordManager) {
    super("HTTP-Proxy");
    this.port = port;
    this.eventBroadcaster = eventBroadcaster;
    this.sessionDao = sessionDao;
    this.eventDao = eventDao;
    this.userDao = userDao;
    this.appConfig = appConfig;
    this.instancePasswordManager = instancePasswordManager;
  }

  @Override
  public void run() {
    server = new Server(port);
    final CustomServletContextHandler context = new CustomServletContextHandler();
    context.setContextPath("/");
    context.setErrorHandler(new NoBodyErrorHandler());

    // filters
    context.addFilter(new AuthFilter(appConfig, sessionDao), List.of(
      // auth
      "/api/logout",
      "/api/session/refresh",
      "/api/update/default/password",
      // all
      "/api/all/event",
      "/api/all/event/archive",
      "/api/all/event/unarchive",
      // single
      "/api/single/event",
      "/api/single/event/read",
      // bulk
      "/api/bulk/event",
      "/api/bulk/event/archive",
      "/api/bulk/event/unarchive",
      // stream
      "/stream/handshake",
      "/stream/events"
    ));
    context.addFilter(new CharacterEncodingFilter(), "/*");
    context.addFilter(new IdCheckerFilter(), "/api/single/*");
    context.addFilter(new MultipleIdsCheckerFilter(), "/api/bulk/*");
    context.addFilter(new EventSourceCheckerFilter(eventDao), "/api/all/*");
    context.addFilter(new EventTableSourceCheckerFilter(), List.of(
      "/api/all/event",
      "/api/single/event",
      "/api/single/event/read",
      "/api/bulk/event"
    ));

    // servlets
    context.addServlet(
      new LoginServlet(appConfig, instancePasswordManager, userDao, sessionDao),
      "/api/login"
    );
    context.addServlet(new LogoutServlet(sessionDao), "/api/logout");
    context.addServlet(new SessionRefreshServlet(), "/api/session/refresh");
    context.addServlet(
      new UpdateDefaultPasswordServlet(userDao, instancePasswordManager),
      "/api/update/default/password"
    );
    context.addServlet(new AllEventServlet(eventDao), "/api/all/event");
    context.addServlet(new AllEventArchiveServlet(eventDao), "/api/all/event/archive");
    context.addServlet(new AllEventUnarchiveServlet(eventDao), "/api/all/event/unarchive");
    context.addServlet(new EventServlet(eventDao), "/api/single/event");
    context.addServlet(new MakeEventReadServlet(eventDao), "/api/single/event/read");
    context.addServlet(new BulkEventServlet(eventDao), "/api/bulk/event");
    context.addServlet(new BulkEventArchiveServlet(eventDao), "/api/bulk/event/archive");
    context.addServlet(new BulkEventUnarchiveServlet(eventDao), "/api/bulk/event/unarchive");
    context.addServlet(new SseHandshakeServlet(eventBroadcaster), "/stream/handshake");
    context.addServlet(new SseServlet(eventBroadcaster), "/stream/events");

    server.setHandler(context);
    try {
      server.start();
      server.join();
    } catch (Exception ex) {
      LOG.error("Unable to start HTTP proxy server. Cause: {}", ex.getMessage());
    }
  }

  @Override
  protected void beforeStopThread() {
    if (server == null) {
      return;
    }
    try {
      server.stop();
    } catch (Exception ignored) {
    }
    LOG.info("HTTP proxy server has been stopped");
  }
}
