package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.db.InstancePasswordManager;
import pl.miloszgilga.event.proxy.server.db.dao.UserDao;
import pl.miloszgilga.event.proxy.server.http.ReqAttribute;

import java.util.Objects;

public class UpdateDefaultPasswordServlet extends HttpServlet {
  private final UserDao userDao;
  private final InstancePasswordManager instancePasswordManager;

  public UpdateDefaultPasswordServlet(UserDao userDao,
                                      InstancePasswordManager instancePasswordManager) {
    this.userDao = userDao;
    this.instancePasswordManager = instancePasswordManager;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
    final String username = (String) req.getAttribute(ReqAttribute.USERNAME.name());
    if (!userDao.userHasDefaultPassword(username)) {
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return; // default password already changed
    }
    final String newPassword = Objects.requireNonNull(req.getParameter("password"));
    final String passwordHash = instancePasswordManager.hash(newPassword);
    userDao.updateUserPassword(username, passwordHash, false);
    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}
