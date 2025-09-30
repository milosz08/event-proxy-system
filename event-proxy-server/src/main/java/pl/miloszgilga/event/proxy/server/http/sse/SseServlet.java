package pl.miloszgilga.event.proxy.server.http.sse;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.crypto.Crypto;
import pl.miloszgilga.event.proxy.server.http.ReqAttribute;

import javax.crypto.SecretKey;
import java.security.PublicKey;

public class SseServlet extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(SseServlet.class);

  private final EventBroadcaster eventBroadcaster;

  public SseServlet(EventBroadcaster eventBroadcaster) {
    this.eventBroadcaster = eventBroadcaster;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) {
    final String pubKeyBase64 = (String) req.getAttribute(ReqAttribute.PUBLIC_KEY.name());
    try {
      final SecretKey aesKey = Crypto.createAesKey();
      final PublicKey pubKey = Crypto.reconstructPubKey(pubKeyBase64);
      final String encryptedAes = Crypto.encryptAesKey(aesKey, pubKey);

      res.setContentType("text/event-stream");
      res.setHeader("Cache-Control", "no-cache");
      res.setHeader("Connection", "keep-alive");

      final String clientId = eventBroadcaster.addClientIdToRequest(req);

      final AsyncContext asyncContext = req.startAsync(req, res);
      asyncContext.setTimeout(0); // set no limit for SSE connections
      eventBroadcaster.addClient(clientId, asyncContext, new HybridKey(aesKey, encryptedAes));

      final CustomAsyncContextListener listener = new CustomAsyncContextListener(
        asyncContext,
        eventBroadcaster
      );
      asyncContext.addListener(listener);
      LOG.info("Client {} connected. Active clients: {}", clientId,
        eventBroadcaster.getClientsCount());
    } catch (Exception ex) {
      res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }
  }
}
