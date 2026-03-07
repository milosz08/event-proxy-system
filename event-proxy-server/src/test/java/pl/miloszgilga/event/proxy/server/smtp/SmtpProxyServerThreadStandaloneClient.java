package pl.miloszgilga.event.proxy.server.smtp;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import pl.miloszgilga.event.proxy.server.TestData;
import pl.miloszgilga.event.proxy.server.TestDataPayload;

import java.util.Properties;

class SmtpProxyServerThreadStandaloneClient {
  private static final String HOST = "0.0.0.0";
  private static final int PORT = 4366;

  public static void main(String[] args) throws MessagingException {
    final SmtpProxyServerThreadStandaloneClient main = new SmtpProxyServerThreadStandaloneClient();

    final TestDataPayload nas = TestData.NAS.apply(null);
    main.sendEmail(nas.subject(), nas.rawBody(), nas.from());

    final TestDataPayload dvr = TestData.DVR.apply("2", null);
    main.sendEmail(dvr.subject(), dvr.rawBody(), dvr.from());
  }

  private void sendEmail(String subject, String body, String from)
    throws MessagingException {
    final String to = "receiver@test";

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
  }
}
