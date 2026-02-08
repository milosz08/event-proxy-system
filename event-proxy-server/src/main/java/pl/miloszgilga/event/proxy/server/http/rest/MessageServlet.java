package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.db.dto.MessageContentWithBody;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;

public class MessageServlet extends HttpJsonServlet {
  private final EventDao eventDao;

  public MessageServlet(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  protected String doJsonGet(HttpServletRequest req, HttpServletResponse res) {
    final String eventSource = (String) req.getAttribute("eventSource");
    final long id = (Long) req.getAttribute("id");

    final MessageContentWithBody messageContent = eventDao.getSingleById(eventSource, id);
    if (messageContent == null) {
      return null;
    }
    final JSONObject element = new JSONObject();
    element.put("id", messageContent.id());
    element.put("subject", messageContent.subject());
    element.put("rawBody", messageContent.rawBody());
    element.put("eventTime", messageContent.eventTime());
    return element.toString();
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
    final String eventSource = (String) req.getAttribute("eventSource");
    final long id = (Long) req.getAttribute("id");
    final boolean success = eventDao.deleteSingleById(id);
    res.setStatus(success
      ? HttpServletResponse.SC_NO_CONTENT
      : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
