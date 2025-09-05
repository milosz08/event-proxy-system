package pl.miloszgilga.event.proxy.server;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

class IdCheckerFilter extends HttpFilter {
  @Override
  protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
    throws IOException, ServletException {
    try {
      final long id = Long.parseLong(req.getParameter("id"));
      req.setAttribute("id", id);
      chain.doFilter(req, res);
    } catch (NumberFormatException ignored) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
