package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;
import pl.miloszgilga.event.proxy.server.http.I18n;
import pl.miloszgilga.event.proxy.server.parser.EmailParser;
import pl.miloszgilga.event.proxy.server.parser.FieldType;

import java.util.Locale;
import java.util.Map;

public class MessageServlet extends HttpJsonServlet {
  private final I18n i18n;
  private final EventDao eventDao;
  private final Map<String, EmailParser> emailParsers;

  public MessageServlet(I18n i18n, EventDao eventDao, Map<String, EmailParser> emailParsers) {
    this.i18n = i18n;
    this.eventDao = eventDao;
    this.emailParsers = emailParsers;
  }

  @Override
  protected String doJsonGet(HttpServletRequest req, HttpServletResponse res) {
    final String eventSource = (String) req.getAttribute("eventSource");
    final long id = (Long) req.getAttribute("id");
    final Locale locale = req.getLocale();

    final Map<String, Object> eventRow = eventDao.getSingleById(eventSource, id);
    if (eventRow == null) {
      return null;
    }
    final EmailParser emailParser = emailParsers.get(eventSource);
    final Map<String, FieldType> parserFields = emailParser.declareParserFields();

    final JSONObject element = new JSONObject();
    for (final Map.Entry<String, Object> rowEntry : eventRow.entrySet()) {
      final FieldType fieldType = parserFields.getOrDefault(rowEntry.getKey(), null);
      if (fieldType != null) {
        final JSONObject rowObject = new JSONObject();
        rowObject.put("value", rowEntry.getValue());
        rowObject.put("type", fieldType.name());
        rowObject.put("i18n", i18n.getMessage(eventSource + "." + rowEntry.getKey(), locale));
        element.put(rowEntry.getKey(), rowObject);
      } else {
        element.put(rowEntry.getKey(), rowEntry.getValue());
      }
    }
    return element.toString();
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
    final String eventSource = (String) req.getAttribute("eventSource");
    final long id = (Long) req.getAttribute("id");
    eventDao.deleteSingleById(eventSource, id);
    res.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}
