package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;

public class MakeEventReadServlet extends HttpJsonServlet {
  private final EventDao eventDao;

  public MakeEventReadServlet(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) {
    final long id = (Long) req.getAttribute("id");
    final boolean success = eventDao.makeEventRead(id);
    res.setStatus(success
      ? HttpServletResponse.SC_NO_CONTENT
      : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
