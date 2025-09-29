package pl.miloszgilga.event.proxy.server.http.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.AppConfig;
import pl.miloszgilga.event.proxy.server.Constants;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;
import pl.miloszgilga.event.proxy.server.http.ReqAttribute;
import pl.miloszgilga.event.proxy.server.http.SessionData;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

public class AuthFilter extends HttpFilter {
  private final AppConfig appConfig;
  private final SessionDao sessionDao;

  public AuthFilter(AppConfig appConfig, SessionDao sessionDao) {
    this.appConfig = appConfig;
    this.sessionDao = sessionDao;
  }

  @Override
  protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
    throws IOException, ServletException {
    final Cookie[] cookies = req.getCookies();
    try {
      if (cookies == null || cookies.length == 0) {
        throw new LoginException(); // no cookies
      }
      final Optional<Cookie> sessionCookie = Arrays.stream(cookies)
        .filter(c -> c.getName().equals(Constants.COOKIE_NAME))
        .findFirst();

      if (sessionCookie.isEmpty()) {
        throw new LoginException(); // no session cookie
      }
      final String sessionId = sessionCookie.get().getValue();
      final SessionData currentSession = sessionDao.getSession(sessionId);
      if (currentSession == null) {
        throw new LoginException(); // missing or expired session
      }
      final int sessionTtlSec = appConfig.getAsInt(AppConfig.Prop.SESSION_TTL_SEC);
      final Instant updatedSessionTime = Instant.now().plus(Duration.ofSeconds(sessionTtlSec));

      sessionDao.updateSessionTime(sessionId, updatedSessionTime);

      final Cookie cookie = new Cookie(Constants.COOKIE_NAME, sessionId);
      cookie.setHttpOnly(true);
      cookie.setSecure(true);
      cookie.setMaxAge(sessionTtlSec);
      cookie.setPath("/");
      res.addCookie(cookie);

      req.setAttribute(ReqAttribute.SESSION_ID.name(), sessionId);
      req.setAttribute(ReqAttribute.CLIENT_ID.name(), currentSession.clientId());
      req.setAttribute(ReqAttribute.PUBLIC_KEY.name(), currentSession.publicKey());
      req.setAttribute(ReqAttribute.USERNAME.name(), currentSession.username());

      chain.doFilter(req, res);
    } catch (LoginException ex) {
      res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
  }
}
