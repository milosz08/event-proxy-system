package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.Utils;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;
import pl.miloszgilga.event.proxy.server.http.Page;

import java.util.Map;

public class MessageAllServlet extends HttpJsonServlet {
  private final EventDao eventDao;

  public MessageAllServlet(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  protected String doJsonGet(HttpServletRequest req, HttpServletResponse res) {
    final String eventSource = (String) req.getAttribute("eventSource");
    final int limit = Utils.safetyParseInt(req.getParameter("limit"), 0);
    final int offset = Utils.safetyParseInt(req.getParameter("offset"), 0);

    final Page<Map<String, Object>> all = eventDao.getAllByEventSource(eventSource, limit, offset);

    final JSONObject root = new JSONObject();
    root.put("totalElements", all.totalElements());
    root.put("hasNext", all.hasNext());

    final JSONArray elements = new JSONArray();
    for (final Map<String, Object> rowElement : all.elements()) {
      final JSONObject rowObject = new JSONObject();
      for (final Map.Entry<String, Object> rowEntry : rowElement.entrySet()) {
        rowObject.put(rowEntry.getKey(), rowEntry.getValue());
      }
      elements.put(rowObject);
    }
    root.put("elements", elements);
    return root.toString();
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
    final String eventSource = (String) req.getAttribute("eventSource");
    eventDao.deleteAllByEventSource(eventSource);
    res.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}
