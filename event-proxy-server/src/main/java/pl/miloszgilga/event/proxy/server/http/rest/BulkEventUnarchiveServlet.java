package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;

public class BulkEventUnarchiveServlet extends HttpServlet {
  private final EventDao eventDao;

  public BulkEventUnarchiveServlet(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) {
    final long[] ids = (long[]) req.getAttribute("ids");
    final boolean success = eventDao.unarchiveMultipleByIds(ids);
    res.setStatus(success
      ? HttpServletResponse.SC_NO_CONTENT
      : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
