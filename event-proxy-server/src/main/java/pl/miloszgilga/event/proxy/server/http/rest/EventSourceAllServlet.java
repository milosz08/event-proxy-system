package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.http.I18n;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;
import pl.miloszgilga.event.proxy.server.parser.EmailParser;

import java.util.List;
import java.util.Locale;

public class EventSourceAllServlet extends HttpJsonServlet {
  private final I18n i18n;
  private final List<EmailParser> emailParsers;

  public EventSourceAllServlet(I18n i18n, List<EmailParser> emailParsers) {
    this.i18n = i18n;
    this.emailParsers = emailParsers;
  }

  @Override
  protected String doJsonGet(HttpServletRequest req, HttpServletResponse res) {
    final Locale locale = req.getLocale();
    final JSONArray eventSources = new JSONArray();
    for (final EmailParser emailParser : emailParsers) {
      final JSONObject eventSource = new JSONObject();
      eventSource.put("name", emailParser.parserName());
      eventSource.put("i18n", i18n.getMessage(emailParser.parserName() + ".subject", locale));
      eventSources.put(eventSource);
    }
    return eventSources.toString();
  }
}
