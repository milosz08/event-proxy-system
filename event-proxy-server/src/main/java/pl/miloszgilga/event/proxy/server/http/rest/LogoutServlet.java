package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.Constants;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;
import pl.miloszgilga.event.proxy.server.http.ReqAttribute;

public class LogoutServlet extends HttpServlet {
  private final SessionDao sessionDao;

  public LogoutServlet(SessionDao sessionDao) {
    this.sessionDao = sessionDao;
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
    final String sessionId = (String) req.getAttribute(ReqAttribute.SESSION_ID.name());
    sessionDao.destroySession(sessionId);

    // invalidate cookie after delete session
    final Cookie cookieToDelete = new Cookie(Constants.COOKIE_NAME, null);
    cookieToDelete.setMaxAge(0);
    res.addCookie(cookieToDelete);

    res.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}
