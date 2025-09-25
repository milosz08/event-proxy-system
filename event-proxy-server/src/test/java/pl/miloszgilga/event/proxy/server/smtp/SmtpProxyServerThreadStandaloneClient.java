package pl.miloszgilga.event.proxy.server.smtp;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import pl.miloszgilga.event.proxy.server.AppConfig;
import pl.miloszgilga.event.proxy.server.parser.EmailParser;
import pl.miloszgilga.event.proxy.server.parser.message.DvrEmailParser;
import pl.miloszgilga.event.proxy.server.parser.message.NasEmailParser;

import java.util.Properties;

class SmtpProxyServerThreadStandaloneClient {
  private static final String HOST = "0.0.0.0";
  private static final int PORT = 4366;

  public static void main(String[] args) throws MessagingException {
    final AppConfig appConfig = new AppConfig();
    final SmtpProxyServerThreadStandaloneClient main = new SmtpProxyServerThreadStandaloneClient();

    main.sendEmail(
      "dlink-02C972_E-Mail_Alert",
      """
        A SMART Test Was Performed On The Following Hard Drive At 03:2:10 On 27-July-2025.

        Device Model:  WDC WD20EFZX-68AWUN0
        Serial Number:  RESTRICTED
        Size: 2,000G

        The Result Of The Test Is: Pass

        Sincerely,
        Your dlink-02C972
        """,
      new NasEmailParser(appConfig)
    );
    main.sendEmail(
      "Embedded Net DVR: Motion Detected On Channel A2",
      """
        This is an automatically generated e-mail from your DVR.

        EVENT TYPE:    Motion Detected
        EVENT TIME:    2025-07-26,16:53:09
        DVR NAME:      Embedded Net DVR
        DVR S/N:       RESTRICTED
        CAMERA NAME(NUM):   CAM 2 Garage(A2)
        """,
      new DvrEmailParser(appConfig)
    );
  }

  private void sendEmail(String subject, String body, EmailParser parser)
    throws MessagingException {
    final String from = parser.senderName();
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
