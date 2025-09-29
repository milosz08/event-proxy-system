package pl.miloszgilga.event.proxy.server.http.rest;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SessionRefreshServlet extends HttpServlet {
  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse res) {
    res.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}
