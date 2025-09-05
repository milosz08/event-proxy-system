package pl.miloszgilga.event.proxy.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class LogoutServlet extends HttpServlet {
  private final SessionDao sessionDao;

  LogoutServlet(SessionDao sessionDao) {
    this.sessionDao = sessionDao;
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
  }
}
