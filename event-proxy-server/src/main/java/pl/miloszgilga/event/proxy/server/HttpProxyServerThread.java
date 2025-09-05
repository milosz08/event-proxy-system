package pl.miloszgilga.event.proxy.server;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class HttpProxyServerThread extends AbstractThread {
  private static final Logger LOG = LoggerFactory.getLogger(HttpProxyServerThread.class);

  private final int port;
  private final EventBroadcaster eventBroadcaster;
  private final SessionDao sessionDao;
  private final EventDao eventDao;
  private final List<EmailParser> emailParsers;
  private final I18n i18n;
  private final Map<String, EmailParser> emailParserMap;

  private Server server;

  HttpProxyServerThread(int port, EventBroadcaster eventBroadcaster, SessionDao sessionDao,
                        EventDao eventDao, List<EmailParser> emailParsers, I18n i18n) {
    super("HTTP-Proxy");
    this.port = port;
    this.eventBroadcaster = eventBroadcaster;
    this.sessionDao = sessionDao;
    this.eventDao = eventDao;
    this.emailParsers = emailParsers;
    this.i18n = i18n;
    emailParserMap = emailParsers.stream()
      .collect(Collectors.toMap(EmailParser::parserName, Function.identity()));
  }

  @Override
  public void run() {
    server = new Server(port);
    final CustomServletContextHandler context = new CustomServletContextHandler();
    context.setContextPath("/");

    final var characterEncodingFilter = new CharacterEncodingFilter();
    final var authFilter = new AuthFilter();
    final var idCheckerFilter = new IdCheckerFilter();
    final var eventSourceCheckerFilter = new EventSourceCheckerFilter(emailParsers);

    final var sseServlet = new SseServlet(eventBroadcaster);
    final var loginServlet = new LoginServlet(sessionDao);
    final var logoutServlet = new LogoutServlet(sessionDao);
    final var sessionRefreshServlet = new SessionRefreshServlet(sessionDao);
    final var messageAllServlet = new MessageAllServlet(eventDao);
    final var messageServlet = new MessageServlet(i18n, eventDao, emailParserMap);
    final var eventSourceAllServlet = new EventSourceAllServlet(i18n, emailParsers);

    context.addFilter(authFilter, List.of(
      "/api/event/source/all",
      "/api/message/*",
      "/api/logout"
    ));
    context.addFilter(characterEncodingFilter, "/*");
    context.addFilter(idCheckerFilter, "/api/message");
    context.addFilter(eventSourceCheckerFilter, "/api/message/*");

    context.addServlet(sseServlet, "/events");
    context.addServlet(loginServlet, "/api/login");
    context.addServlet(logoutServlet, "/api/logout");
    context.addServlet(sessionRefreshServlet, "/api/session/refresh");
    context.addServlet(eventSourceAllServlet, "/api/event/source/all");
    context.addServlet(messageAllServlet, "/api/message/all");
    context.addServlet(messageServlet, "/api/message");

    server.setHandler(context);
    try {
      server.start();
      server.join();
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
