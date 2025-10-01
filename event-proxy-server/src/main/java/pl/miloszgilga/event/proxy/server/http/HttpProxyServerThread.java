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
import pl.miloszgilga.event.proxy.server.http.sse.SseServlet;
import pl.miloszgilga.event.proxy.server.parser.EmailParser;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HttpProxyServerThread extends AbstractThread {
  private static final Logger LOG = LoggerFactory.getLogger(HttpProxyServerThread.class);

  private final int port;
  private final EventBroadcaster eventBroadcaster;
  private final SessionDao sessionDao;
  private final EventDao eventDao;
  private final UserDao userDao;
  private final List<EmailParser> emailParsers;
  private final I18n i18n;
  private final AppConfig appConfig;
  private final InstancePasswordManager instancePasswordManager;
  private final Map<String, EmailParser> emailParserMap;

  private Server server;

  public HttpProxyServerThread(int port, EventBroadcaster eventBroadcaster, SessionDao sessionDao,
                               EventDao eventDao, UserDao userDao, List<EmailParser> emailParsers,
                               AppConfig appConfig, InstancePasswordManager instancePasswordManager,
                               I18n i18n) {
    super("HTTP-Proxy");
    this.port = port;
    this.eventBroadcaster = eventBroadcaster;
    this.sessionDao = sessionDao;
    this.eventDao = eventDao;
    this.userDao = userDao;
    this.emailParsers = emailParsers;
    this.i18n = i18n;
    this.appConfig = appConfig;
    this.instancePasswordManager = instancePasswordManager;
    emailParserMap = emailParsers.stream()
      .collect(Collectors.toMap(EmailParser::parserName, Function.identity()));
  }

  @Override
  public void run() {
    server = new Server(port);
    final CustomServletContextHandler context = new CustomServletContextHandler();
    context.setContextPath("/");
    context.setErrorHandler(new NoBodyErrorHandler());

    context.addFilter(new AuthFilter(appConfig, sessionDao), List.of(
      "/api/logout",
      "/api/message/*",
      "/api/session/refresh",
      "/api/update/default/password",
      "/api/event/source/all",
      "/stream/events"
    ));
    context.addFilter(new CharacterEncodingFilter(), "/*");
    context.addFilter(new IdCheckerFilter(), "/api/message");
    context.addFilter(new EventSourceCheckerFilter(emailParsers), "/api/message/*");

    context.addServlet(new EventSourceAllServlet(i18n, emailParsers), "/api/event/source/all");
    context.addServlet(
      new LoginServlet(appConfig, instancePasswordManager, userDao, sessionDao),
      "/api/login"
    );
    context.addServlet(new LogoutServlet(sessionDao), "/api/logout");
    context.addServlet(new MessageAllServlet(eventDao), "/api/message/all");
    context.addServlet(new MessageServlet(i18n, eventDao, emailParserMap), "/api/message");
    context.addServlet(new SessionRefreshServlet(), "/api/session/refresh");
    context.addServlet(
      new UpdateDefaultPasswordServlet(userDao, instancePasswordManager),
      "/api/update/default/password"
    );
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
