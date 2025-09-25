package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.db.dao.SessionDao;

public class LoginServlet extends HttpServlet {
  private final SessionDao sessionDao;

  public LoginServlet(SessionDao sessionDao) {
    this.sessionDao = sessionDao;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) {
  }
}
