package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.Utils;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.db.dto.EventContent;
import pl.miloszgilga.event.proxy.server.http.EventTableSource;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;
import pl.miloszgilga.event.proxy.server.http.Page;

public class AllEventServlet extends HttpJsonServlet {
  private final EventDao eventDao;

  public AllEventServlet(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  protected String doJsonGet(HttpServletRequest req, HttpServletResponse res) {
    final String eventSource = (String) req.getAttribute("eventSource");
    final EventTableSource tableSource = (EventTableSource) req.getAttribute("eventTable");

    final int limit = Utils.safetyParseInt(req.getParameter("limit"), 0);
    final int offset = Utils.safetyParseInt(req.getParameter("offset"), 0);

    final Page<EventContent> all = eventDao.getAllByOptionalEventSource(
      tableSource, eventSource, limit, offset
    );

    final JSONObject root = new JSONObject();
    root.put("totalElements", all.totalElements());
    root.put("hasNext", all.hasNext());

    final JSONArray elements = new JSONArray();
    for (final EventContent rowElement : all.elements()) {
      final JSONObject rowObject = new JSONObject();
      rowObject.put("id", rowElement.id());
      rowObject.put("subject", rowElement.subject());
      rowObject.put("eventTime", rowElement.eventTime());
      rowObject.put("eventSource", rowElement.eventSource());
      rowObject.put("isUnread", rowElement.isUnread());
      elements.put(rowObject);
    }
    root.put("elements", elements);
    return root.toString();
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
    final String eventSource = (String) req.getAttribute("eventSource");
    final EventTableSource tableSource = (EventTableSource) req.getAttribute("eventTable");

    final boolean success = eventDao.deleteAllByOptionalEventSource(tableSource, eventSource);
    res.setStatus(success
      ? HttpServletResponse.SC_NO_CONTENT
      : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
