package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.AppConfig;
import pl.miloszgilga.event.proxy.server.Utils;
import pl.miloszgilga.event.proxy.server.db.InstancePasswordManager;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class LoginServlet extends HttpJsonServlet {
  private final AppConfig appConfig;
  private final InstancePasswordManager instancePasswordManager;
  private final SessionDao sessionDao;

  public LoginServlet(AppConfig appConfig, InstancePasswordManager instancePasswordManager,
                      SessionDao sessionDao) {
    this.appConfig = appConfig;
    this.instancePasswordManager = instancePasswordManager;
    this.sessionDao = sessionDao;
  }

  @Override
  protected String doJsonPost(HttpServletRequest req, HttpServletResponse res) {
    final String username = req.getParameter("username");
    final String password = req.getParameter("password");

    if (!instancePasswordManager.verify(username, password)) {
      res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return null;
    }
    final String clientId = Objects.requireNonNull(req.getParameter("clientId"));
    final String pubKey = Objects.requireNonNull(req.getParameter("pubKey"));

    final int sessionTtlSec = appConfig.getAsInt(AppConfig.Prop.SESSION_TTL_SEC);

    final String sessionId = UUID.randomUUID().toString();
    final String publicKeySha256 = Utils.generateSha256(pubKey);
    final Instant expiresAt = Instant.now().plus(Duration.ofSeconds(sessionTtlSec));

    sessionDao.createSession(sessionId, clientId, pubKey, publicKeySha256, expiresAt);

    final Cookie cookie = new Cookie("sid", sessionId);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setMaxAge(sessionTtlSec);

    res.addCookie(cookie);

    final JSONObject root = new JSONObject();
    root.put("hasDefaultPassword", instancePasswordManager.hasDefaultPassword());
    return root.toString();
  }
}
