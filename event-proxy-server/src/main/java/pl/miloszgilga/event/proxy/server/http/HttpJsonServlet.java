package pl.miloszgilga.event.proxy.server.http;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Writer;

public abstract class HttpJsonServlet extends HttpServlet {
  @Override
  protected final void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    performJsonRequest(res, doJsonGet(req, res));
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    performJsonRequest(res, doJsonPost(req, res));
  }

  protected String doJsonGet(HttpServletRequest req, HttpServletResponse res) {
    return null;
  }

  protected String doJsonPost(HttpServletRequest req, HttpServletResponse res) {
    return null;
  }

  private void performJsonRequest(HttpServletResponse res, String jsonData) throws IOException {
    if (jsonData == null) {
      return;
    }
    res.setHeader("Content-Type", "application/json");
    final Writer writer = res.getWriter();
    writer.write(jsonData);
    writer.flush();
  }
}
