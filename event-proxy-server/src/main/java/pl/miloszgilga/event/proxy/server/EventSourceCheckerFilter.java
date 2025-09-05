package pl.miloszgilga.event.proxy.server;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

class EventSourceCheckerFilter extends HttpFilter {
  private final List<EmailParser> emailParsers;

  EventSourceCheckerFilter(List<EmailParser> emailParsers) {
    this.emailParsers = emailParsers;
  }

  @Override
  public void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
    throws IOException, ServletException {
    final String eventSource = req.getParameter("eventSource");

    final boolean eventSourceExists = emailParsers.stream()
      // eventSource might be null, so we must check via Objects.equals()
      .anyMatch(parser -> Objects.equals(eventSource, parser.parserName()));

    if (!eventSourceExists) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    req.setAttribute("eventSource", eventSource);
    chain.doFilter(req, res);
  }
}
