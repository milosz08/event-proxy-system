package pl.miloszgilga.event.proxy.server.smtp;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.miloszgilga.event.proxy.server.parser.EmailContent;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SmtpProxyServerThreadTest {
  private static final String HOST = "localhost";
  private static final int PORT = 2526;

  private BlockingQueue<EmailContent> queue;
  private SmtpProxyServerThread smtpProxyServerThread;

  @BeforeEach
  void setUp() {
    queue = new ArrayBlockingQueue<>(1);
    smtpProxyServerThread = new SmtpProxyServerThread(PORT, 1, queue);
    smtpProxyServerThread.start();
  }

  @Test
  void shouldReceiveEmailSentByClient() throws MessagingException, InterruptedException {
    final String from = "sender@test";
    final String to = "receiver@test";
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
      —, „“, ©, ®""";

    sendMessageAndAssert(from, to, subject, body);
  }

  @Test
  void shouldReceiveDvrMessageBody() throws MessagingException, InterruptedException {
    final String from = "dvr-in@event-test";
    final String to = "dvr-out@event-test";
    final String subject = "Dvr event test topic";
    final String body = """
      This is an automatically generated e-mail from your DVR.

      EVENT TYPE:    Motion Detected
      EVENT TIME:    2025-07-27,11:41:29
      DVR NAME:      Embedded Net DVR
      DVR S/N:       RESTRICTED
      CAMERA NAME(NUM):   CAM 2 Garage(A2)""";

    sendMessageAndAssert(from, to, subject, body);
  }

  @Test
  void shouldReceiveNasMessageBody() throws MessagingException, InterruptedException {
    final String from = "nas-in@event-test";
    final String to = "nas-out@event-test";
    final String subject = "Nas event test topic";
    final String body = """
      A SMART Test Was Performed On The Following Hard Drive At 03:2:10 On 27-July-2025.

      Device Model:  WDC WD20EFZX-68AWUN0
      Serial Number:  RESTRICTED
      Size: 2,000G

      The Result Of The Test Is: Pass

      Sincerely,
      Your dlink-02C972""";

    sendMessageAndAssert(from, to, subject, body);
  }

  @AfterEach
  public void tearDown() {
    smtpProxyServerThread.stop();
  }

  private void sendMessageAndAssert(String from, String to, String subject, String body)
    throws MessagingException, InterruptedException {

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

    final EmailContent emailContent = queue.take();

    assertNotNull(emailContent, "email content cannot be null");
    assertEquals(from, emailContent.from(), "incorrect sender email address");
    assertEquals(subject, emailContent.subject(), "incorrect message subject");
    assertEquals(body, normalizeLineEndings(emailContent.rawBody()), "incorrect message body");
  }

  private String normalizeLineEndings(String str) {
    return str.replace("\r\n", "\n");
  }
}
