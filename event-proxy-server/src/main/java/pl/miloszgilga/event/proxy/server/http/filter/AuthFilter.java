package pl.miloszgilga.event.proxy.server.http.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class AuthFilter extends HttpFilter {
  @Override
  protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
    throws IOException, ServletException {
    // TODO: checking cookie and refresh session, if authenticated send forward otherwise 401
    chain.doFilter(req, res);
  }
}
