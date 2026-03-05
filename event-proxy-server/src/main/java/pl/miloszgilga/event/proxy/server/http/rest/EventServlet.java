package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.db.dto.EventContentWithBody;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;

public class EventServlet extends HttpJsonServlet {
  private final EventDao eventDao;

  public EventServlet(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  protected String doJsonGet(HttpServletRequest req, HttpServletResponse res) {
    final long id = (Long) req.getAttribute("id");
    final EventContentWithBody eventContent = eventDao.getSingleById(id);
    if (eventContent == null) {
      return null;
    }
    final JSONObject element = new JSONObject();
    element.put("id", eventContent.id());
    element.put("subject", eventContent.subject());
    element.put("rawBody", eventContent.rawBody());
    element.put("eventTime", eventContent.eventTime());
    return element.toString();
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
    final long id = (Long) req.getAttribute("id");
    final boolean success = eventDao.deleteSingleById(id);
    res.setStatus(success
      ? HttpServletResponse.SC_NO_CONTENT
      : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
