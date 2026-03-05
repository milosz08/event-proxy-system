package pl.miloszgilga.event.proxy.server.http.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.miloszgilga.event.proxy.server.http.EventTableSource;

import java.io.IOException;

public class EventTableSourceCheckerFilter extends HttpFilter {
  @Override
  public void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
    throws IOException, ServletException {
    final String eventTable = req.getParameter("eventTable");
    final EventTableSource tableSource = EventTableSource.fromString(eventTable);
    if (tableSource == null) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    req.setAttribute("eventTable", tableSource);
    chain.doFilter(req, res);
  }
}
