package pl.miloszgilga.event.proxy.server.http;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Writer;

public abstract class HttpJsonServlet extends HttpServlet {
  @Override
  protected final void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException {
    final String jsonData = doJsonGet(req, res);
    if (jsonData == null) {
      return;
    }
    res.setHeader("Content-Type", "application/json");
    final Writer writer = res.getWriter();
    writer.write(jsonData);
    writer.flush();
  }

  protected abstract String doJsonGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException;
}
