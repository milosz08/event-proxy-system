package pl.miloszgilga.event.proxy.server;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class SmtpProxyServerThreadStandaloneClient {
  private static final String HOST = "localhost";
  private static final int PORT = 1025;

  private final BlockingQueue<EmailContent> queue;
  private final SmtpProxyServerThread smtpProxyServerThread;

  SmtpProxyServerThreadStandaloneClient() {
    queue = new ArrayBlockingQueue<>(1);
    smtpProxyServerThread = new SmtpProxyServerThread(PORT, 1, queue);
  }

  void sendEmail() throws MessagingException {
    smtpProxyServerThread.start();

    final String from = "nadawca@test";
    final String to = "odbiorca@test";
    final String subject = "Zażółć gęślą - jaźń. Zażółć gęślą jaźń";
    final String body = "Testing message";

    final Properties props = new Properties();
    props.put("mail.smtp.host", HOST);
    props.put("mail.smtp.port", String.valueOf(PORT));
    final Session session = Session.getInstance(props);

    final MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
    message.setSubject(subject);
    message.setText(body);

    Transport.send(message);

    smtpProxyServerThread.stop();
    queue.clear();
  }

  public static void main(String[] args) throws MessagingException {
    final SmtpProxyServerThreadStandaloneClient main = new SmtpProxyServerThreadStandaloneClient();
    main.sendEmail();
  }
}
