package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;

public class BulkEventServlet extends HttpJsonServlet {
  private final EventDao eventDao;

  public BulkEventServlet(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
    final long[] ids = (long[]) req.getAttribute("ids");
    final boolean success = eventDao.deleteMultipleByIds(ids);
    res.setStatus(success
      ? HttpServletResponse.SC_NO_CONTENT
      : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
