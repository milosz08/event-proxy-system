package pl.miloszgilga.event.proxy.server;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SmtpProxyServerThreadTest {
  private static final String HOST = "localhost";
  private static final int PORT = 2526;

  private SmtpProxyServerThread smtpProxyServerThread;

  @BeforeEach
  void setUp() {
    smtpProxyServerThread = new SmtpProxyServerThread(PORT, 1);
    smtpProxyServerThread.start();
  }

  @Test
  void shouldReceiveEmailSentByClient() throws MessagingException, InterruptedException {
    final String from = "nadawca@test";
    final String to = "odbiorca@test";
    final String subject = "Zażółć gęślą - jaźń. Zażółć gęślą jaźń";
    final String body = """
      ą, ć, ę, ł, ń, ó, ś, ź, ż.
      Ą, Ć, Ę, Ł, Ń, Ó, Ś, Ź, Ż.
      äöüß
      éàçâê
      ñáíú
      ščřžýáíé
      øæå
      Привет, мир! Это тестовое сообщение.
      Καλημέρα, κόσμε!
      こんにちは
      你好
      안녕하세요
      enojis: 👍🎉❤️😂🤔
      $, €, £, ¥
      `!@#$%^&*()_+-=[]{}|;':",./<>?`
      —, „“, ©, ®
      """;

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

    final EmailContent emailContent = smtpProxyServerThread.getNextEmail();

    assertNotNull(emailContent, "email content cannot be null");
    assertEquals(from, emailContent.from(), "incorrect sender email address");
    assertEquals(subject, emailContent.subject(), "incorrect message subject");
    assertEquals(body, emailContent.rawBody(), "incorrect message body");
  }

  @AfterEach
  public void tearDown() {
    smtpProxyServerThread.stop();
  }
}
