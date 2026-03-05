package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;

public class AllEventUnarchiveServlet extends HttpServlet {
  private final EventDao eventDao;

  public AllEventUnarchiveServlet(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) {
    final String eventSource = (String) req.getAttribute("eventSource");
    final boolean success = eventDao.unarchiveAllByOptionalEventSource(eventSource);
    res.setStatus(success
      ? HttpServletResponse.SC_NO_CONTENT
      : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
