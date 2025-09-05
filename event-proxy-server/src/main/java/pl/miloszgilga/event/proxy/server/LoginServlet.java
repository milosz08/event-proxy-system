package pl.miloszgilga.event.proxy.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class LoginServlet extends HttpServlet {
  private final SessionDao sessionDao;

  LoginServlet(SessionDao sessionDao) {
    this.sessionDao = sessionDao;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) {
  }
}
