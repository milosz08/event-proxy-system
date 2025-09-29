package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.AppConfig;
import pl.miloszgilga.event.proxy.server.Constants;
import pl.miloszgilga.event.proxy.server.db.InstancePasswordManager;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;
import pl.miloszgilga.event.proxy.server.db.dao.UserDao;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class LoginServlet extends HttpJsonServlet {
  private final AppConfig appConfig;
  private final InstancePasswordManager instancePasswordManager;
  private final UserDao userDao;
  private final SessionDao sessionDao;

  public LoginServlet(AppConfig appConfig, InstancePasswordManager instancePasswordManager,
                      UserDao userDao, SessionDao sessionDao) {
    this.appConfig = appConfig;
    this.instancePasswordManager = instancePasswordManager;
    this.userDao = userDao;
    this.sessionDao = sessionDao;
  }

  @Override
  protected String doJsonPost(HttpServletRequest req, HttpServletResponse res) {
    final String username = req.getParameter("username");
    final String password = req.getParameter("password");

    if (!instancePasswordManager.verify(username, password)) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return null;
    }
    final String clientId = Objects.requireNonNull(req.getParameter("clientId"));
    final String pubKey = Objects.requireNonNull(req.getParameter("pubKey"));

    final int sessionTtlSec = appConfig.getAsInt(AppConfig.Prop.SESSION_TTL_SEC);

    final String sessionId = UUID.randomUUID().toString();
    final Instant expiresAt = Instant.now().plus(Duration.ofSeconds(sessionTtlSec));

    final Integer userId = userDao.getUserId(username);
    sessionDao.createSession(sessionId, userId, clientId, pubKey, expiresAt);

    final Cookie cookie = new Cookie(Constants.COOKIE_NAME, sessionId);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setMaxAge(sessionTtlSec);
    cookie.setPath("/");

    res.addCookie(cookie);

    final JSONObject root = new JSONObject();
    root.put("hasDefaultPassword", Objects
      .requireNonNullElse(userDao.userHasDefaultPassword(username), false));
    return root.toString();
  }
}
