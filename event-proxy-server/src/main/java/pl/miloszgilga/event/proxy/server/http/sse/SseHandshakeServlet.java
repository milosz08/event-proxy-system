package pl.miloszgilga.event.proxy.server.http.sse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.crypto.Crypto;
import pl.miloszgilga.event.proxy.server.http.HttpJsonServlet;
import pl.miloszgilga.event.proxy.server.http.ReqAttribute;

import javax.crypto.SecretKey;
import java.security.PublicKey;

public class SseHandshakeServlet extends HttpJsonServlet {
  private static final Logger LOG = LoggerFactory.getLogger(SseHandshakeServlet.class);

  private final EventBroadcaster eventBroadcaster;

  public SseHandshakeServlet(EventBroadcaster eventBroadcaster) {
    this.eventBroadcaster = eventBroadcaster;
  }

  @Override
  protected String doJsonPost(HttpServletRequest req, HttpServletResponse res) {
    final String pubKeyBase64 = (String) req.getAttribute(ReqAttribute.PUBLIC_KEY.name());
    String result = null;
    try {
      final SecretKey aesKey = Crypto.createAesKey();
      final PublicKey pubKey = Crypto.reconstructPubKey(pubKeyBase64);
      final String encryptedAes = Crypto.encryptAesKey(aesKey, pubKey);

      final String sessionId = eventBroadcaster.createSessionClient(aesKey);
      LOG.info("Handshake SSE client: {}", sessionId);

      final JSONObject object = new JSONObject();
      object.put("sessionId", sessionId);
      object.put("aesEncrypted", encryptedAes);

      result = object.toString();
    } catch (Exception ex) {
      res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }
    return result;
  }
}
