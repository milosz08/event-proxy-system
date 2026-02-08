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
import pl.miloszgilga.event.proxy.server.http.filter.AuthFilter;
import pl.miloszgilga.event.proxy.server.http.filter.CharacterEncodingFilter;
import pl.miloszgilga.event.proxy.server.http.filter.EventSourceCheckerFilter;
import pl.miloszgilga.event.proxy.server.http.filter.IdCheckerFilter;
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
      "/api/event/source/all",
      "/api/logout",
      "/api/message/all",
      "/api/message/read",
      "/api/message",
      "/api/session/refresh",
      "/api/update/default/password",
      "/stream/handshake",
      "/stream/events"
    ));
    context.addFilter(new CharacterEncodingFilter(), "/*");
    context.addFilter(new IdCheckerFilter(), List.of(
      "/api/message/read",
      "/api/message"
    ));
    context.addFilter(new EventSourceCheckerFilter(eventDao), "/api/message/all");

    // servlets
    context.addServlet(new EventSourceAllServlet(eventDao), "/api/event/source/all");
    context.addServlet(
      new LoginServlet(appConfig, instancePasswordManager, userDao, sessionDao),
      "/api/login"
    );
    context.addServlet(new LogoutServlet(sessionDao), "/api/logout");
    context.addServlet(new MessageAllServlet(eventDao), "/api/message/all");
    context.addServlet(new MakeMessageReadServlet(eventDao), "/api/message/read");
    context.addServlet(new MessageServlet(eventDao), "/api/message");
    context.addServlet(new SessionRefreshServlet(), "/api/session/refresh");
    context.addServlet(
      new UpdateDefaultPasswordServlet(userDao, instancePasswordManager),
      "/api/update/default/password"
    );
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
