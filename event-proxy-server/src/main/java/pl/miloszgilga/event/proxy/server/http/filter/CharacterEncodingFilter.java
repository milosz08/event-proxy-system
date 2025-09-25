package pl.miloszgilga.event.proxy.server.http.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CharacterEncodingFilter extends HttpFilter {
  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
    throws IOException, ServletException {
    req.setCharacterEncoding(StandardCharsets.UTF_8.name());
    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
    chain.doFilter(req, res);
  }
}
