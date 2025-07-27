package pl.miloszgilga.event.proxy.server;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;

class SmtpMessageReceiver implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(SmtpMessageReceiver.class);

  private final Socket clientSocket;
  private final BlockingQueue<EmailContent> queue;

  SmtpMessageReceiver(Socket clientSocket, BlockingQueue<EmailContent> queue) {
    this.clientSocket = clientSocket;
    this.queue = queue;
  }

  @Override
  public void run() {
    LOG.debug("Connected with {}", clientSocket.getInetAddress());
    try(
      final Socket s = clientSocket;
      final BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(),
        StandardCharsets.UTF_8));
      final PrintWriter out = new PrintWriter(s.getOutputStream(), true)
    ) {
      out.println("220 localhost SimpleSMTPServer ready");

      final List<String> rawLines = new ArrayList<>();
      String line;
      while ((line = in.readLine()) != null) {
        rawLines.add(line);
        if (line.equalsIgnoreCase("QUIT")) {
          out.println("221 Bye");
          break;
        } else if (line.toUpperCase().startsWith("DATA")) {
          out.println("354 Start mail input; end with <CRLF>.<CRLF>");
          while (!(line = in.readLine()).equals(".")) {
            rawLines.add(line);
          }
          out.println("250 OK: Queued for delivery");
        } else {
          out.println("250 OK");
        }
      }

      final EmailContent emailContent = readEmail(rawLines);
      if (emailContent != null) {
        queue.put(emailContent);
      }
    } catch (Exception ex) {
      LOG.error("Message processing exception. Cause: {}", ex.getMessage());
    }
  }

  private EmailContent readEmail(List<String> rawLines) throws MessagingException, IOException {
    int dataStartIndex = -1;
    for (int i = 0; i < rawLines.size(); i++) {
      if (rawLines.get(i).equalsIgnoreCase("DATA")) {
        dataStartIndex = i + 1;
        break;
      }
    }
    if (dataStartIndex == -1) {
      LOG.warn("DATA command not found in SMTP session.");
      return null;
    }
    final List<String> emailDataLines = rawLines.subList(dataStartIndex, rawLines.size() - 1);
    final String rawEmail = String.join("\r\n", emailDataLines);

    final InputStream stream = new ByteArrayInputStream(rawEmail.getBytes(StandardCharsets.UTF_8));
    final Properties props = new Properties();
    final Session session = Session.getDefaultInstance(props);
    final Message message = new MimeMessage(session, stream);

    final String from = (message.getFrom() != null && message.getFrom().length > 0)
      ? message.getFrom()[0].toString() : null;
    final String subject = message.getSubject();
    final String body = getTextFromMessage(message);

    return new EmailContent(from, subject, body);
  }

  private String getTextFromMessage(Message message) throws MessagingException, IOException {
    final Deque<Part> partsToProcess = new ArrayDeque<>();
    partsToProcess.push(message);
    while (!partsToProcess.isEmpty()) {
      final Part currentPart = partsToProcess.pop();
      if (currentPart.isMimeType("text/plain")) {
        return (String) currentPart.getContent();
      }
      if (!currentPart.isMimeType("multipart/*")) {
        continue;
      }
      final Multipart multipart = (Multipart) currentPart.getContent();
      final int count = multipart.getCount();
      for (int i = count - 1; i >= 0; i--) {
        final Part childPart = multipart.getBodyPart(i);
        if (!Part.ATTACHMENT.equalsIgnoreCase(childPart.getDisposition())) {
          partsToProcess.push(childPart);
        }
      }
    }
    return null;
  }
}
