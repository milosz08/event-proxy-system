package pl.miloszgilga.event.proxy.server.http;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.crypto.Crypto;
import pl.miloszgilga.event.proxy.server.crypto.EncryptedMessage;

import java.io.IOException;
import java.io.Writer;

public abstract class HttpJsonServlet extends HttpServlet {
  @Override
  protected final void doGet(HttpServletRequest req, HttpServletResponse res) {
    final String plainJsonData = doJsonGet(req, res);
    if (plainJsonData == null) {
      res.setStatus(HttpStatus.NOT_FOUND_404);
      return;
    }
    final String pubKey = (String) req.getAttribute(ReqAttribute.PUBLIC_KEY.name());
    if (pubKey == null) {
      res.setStatus(HttpStatus.UNAUTHORIZED_401);
      return;
    }
    try {
      final EncryptedMessage encryptedMessage = Crypto.encryptData(pubKey, plainJsonData);

      final JSONObject encryptedPackage = new JSONObject();
      encryptedPackage.put("iv", encryptedMessage.iv());
      encryptedPackage.put("aes", encryptedMessage.aes());
      encryptedPackage.put("encrypted", encryptedMessage.jsonContent());

      performJsonRequest(res, encryptedPackage.toString());
    } catch (Exception ignored) {
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
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
