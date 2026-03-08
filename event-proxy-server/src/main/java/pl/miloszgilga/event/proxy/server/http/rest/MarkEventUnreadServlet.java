package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.http.EventTableSource;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;

public class MarkEventUnreadServlet extends HttpJsonServlet {
  private final EventDao eventDao;

  public MarkEventUnreadServlet(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) {
    final long id = (Long) req.getAttribute("id");
    final EventTableSource tableSource = (EventTableSource) req.getAttribute("eventTable");

    final boolean success = eventDao.updateEventReadStatus(tableSource, id, true);
    res.setStatus(success
      ? HttpServletResponse.SC_NO_CONTENT
      : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
