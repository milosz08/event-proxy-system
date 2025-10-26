package pl.miloszgilga.event.proxy.server.http;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import pl.miloszgilga.event.proxy.server.crypto.AesEncryptedBase64Data;
import pl.miloszgilga.event.proxy.server.crypto.Crypto;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.Writer;
import java.security.PublicKey;

public abstract class HttpJsonServlet extends HttpServlet {
  @Override
  protected final void doGet(HttpServletRequest req, HttpServletResponse res) {
    final String plainJsonData = doJsonGet(req, res);
    if (plainJsonData == null) {
      res.setStatus(HttpStatus.NOT_FOUND_404);
      return;
    }
    final String pubKeyBase64 = (String) req.getAttribute(ReqAttribute.PUBLIC_KEY.name());
    if (pubKeyBase64 == null) {
      res.setStatus(HttpStatus.UNAUTHORIZED_401);
      return;
    }
    try {
      final SecretKey aesKey = Crypto.createAesKey(); // generate aes key (once per request)
      // RSA public key reconstruction
      final PublicKey pubKey = Crypto.reconstructPubKey(pubKeyBase64);
      final AesEncryptedBase64Data data = Crypto.encryptDataAes(plainJsonData, aesKey);
      final String encryptedAes = Crypto.encryptAesKey(aesKey, pubKey);

      final JSONObject encryptedPackage = new JSONObject();
      encryptedPackage.put("cipher", data.cipher());
      encryptedPackage.put("iv", data.iv());
      encryptedPackage.put("tag", data.tag());
      encryptedPackage.put("aes", encryptedAes);

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
