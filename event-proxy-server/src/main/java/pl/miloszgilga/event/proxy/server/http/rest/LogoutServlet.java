package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;

public class LogoutServlet extends HttpServlet {
  private final SessionDao sessionDao;

  public LogoutServlet(SessionDao sessionDao) {
    this.sessionDao = sessionDao;
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
    final String sessionId = (String) req.getAttribute("sessionId");
    sessionDao.destroySession(sessionId);

    // invalidate cookie after delete session
    final Cookie cookieToDelete = new Cookie("sid", null);
    cookieToDelete.setMaxAge(0);
    res.addCookie(cookieToDelete);

    res.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}
