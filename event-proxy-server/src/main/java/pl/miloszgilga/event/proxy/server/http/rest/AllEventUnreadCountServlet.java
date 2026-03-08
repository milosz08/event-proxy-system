package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.http.EventTableSource;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;

public class AllEventUnreadCountServlet extends HttpJsonServlet {
  private final EventDao eventDao;

  public AllEventUnreadCountServlet(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  protected String doJsonGet(HttpServletRequest req, HttpServletResponse res) {
    final EventTableSource tableSource = (EventTableSource) req.getAttribute("eventTable");
    final String eventSource = (String) req.getAttribute("eventSource");

    final long countOfUnreadEvents = eventDao.getUnreadEventsCount(tableSource, eventSource);

    final JSONObject element = new JSONObject();
    element.put("count", countOfUnreadEvents);
    return element.toString();
  }
}
