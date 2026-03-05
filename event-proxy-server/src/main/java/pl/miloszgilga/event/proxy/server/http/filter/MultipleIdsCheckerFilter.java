package pl.miloszgilga.event.proxy.server.http.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

public class MultipleIdsCheckerFilter extends HttpFilter {
  @Override
  protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
    throws IOException, ServletException {
    final String[] idStrings = req.getParameterValues("id");
    if (idStrings == null || idStrings.length == 0) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    try {
      final long[] ids = Arrays.stream(idStrings)
        .mapToLong(Long::parseLong)
        .toArray();

      req.setAttribute("ids", ids);
      chain.doFilter(req, res);
    } catch (NumberFormatException e) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
