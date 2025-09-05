package pl.miloszgilga.event.proxy.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class SessionRefreshServlet extends HttpServlet {
  private final SessionDao sessionDao;

  SessionRefreshServlet(SessionDao sessionDao) {
    this.sessionDao = sessionDao;
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
  }
}
