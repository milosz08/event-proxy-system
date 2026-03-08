package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.http.EventTableSource;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;

import java.util.List;

public class AllEventSourceServlet extends HttpJsonServlet {
  private final EventDao eventDao;

  public AllEventSourceServlet(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  protected String doJsonGet(HttpServletRequest req, HttpServletResponse res) {
    final EventTableSource tableSource = (EventTableSource) req.getAttribute("eventTable");
    final List<String> eventSourcesNames = eventDao.getEventSources(tableSource);
    
    final JSONArray eventSources = new JSONArray();
    for (final String eventSourceName : eventSourcesNames) {
      eventSources.put(eventSourceName);
    }
    return eventSources.toString();
  }
}
