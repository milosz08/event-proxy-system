package pl.miloszgilga.event.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

class SmtpMessageReceiver implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(SmtpMessageReceiver.class);

  private final Socket clientSocket;

  SmtpMessageReceiver(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    LOG.debug("Connected with {}", clientSocket.getInetAddress());
    try(
      final BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket
        .getInputStream()));
      final PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
    ) {
      out.println("220 FakeSmtpServer Ready");

      String line;
      while ((line = in.readLine()) != null) {
        // TODO: process email content
        System.out.println(line);
      }

    } catch (IOException ex) {
      LOG.error("Message processing exception. Cause: {}", ex.getMessage());
    } finally {
      try {
        clientSocket.close();
        LOG.debug("Disconnected with {}", clientSocket.getInetAddress());
      } catch (IOException ex) {
        LOG.error("Unable to close client socket. Cause: {}", ex.getMessage());
      }
    }
  }
}
