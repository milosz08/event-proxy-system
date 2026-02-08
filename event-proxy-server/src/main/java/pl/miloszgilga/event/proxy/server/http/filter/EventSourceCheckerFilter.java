package pl.miloszgilga.event.proxy.server.http.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;

import java.io.IOException;

public class EventSourceCheckerFilter extends HttpFilter {
  private final EventDao eventDao;

  public EventSourceCheckerFilter(EventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  public void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
    throws IOException, ServletException {
    final String eventSource = req.getParameter("eventSource");
    // eventSource might be null
    if (eventSource == null || !eventDao.eventSourceExists(eventSource)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    req.setAttribute("eventSource", eventSource);
    chain.doFilter(req, res);
  }
}
