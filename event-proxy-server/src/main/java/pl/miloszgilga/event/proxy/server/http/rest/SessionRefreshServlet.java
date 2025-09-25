package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;

public class SessionRefreshServlet extends HttpServlet {
  private final SessionDao sessionDao;

  public SessionRefreshServlet(SessionDao sessionDao) {
    this.sessionDao = sessionDao;
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
  }
}
