package pl.miloszgilga.event.proxy.server.parser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.miloszgilga.event.proxy.server.AppConfig;
import pl.miloszgilga.event.proxy.server.parser.message.DvrEmailParser;
import pl.miloszgilga.event.proxy.server.parser.message.NasEmailParser;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmailParserTest {
  private static AppConfig appConfig;

  @BeforeAll
  static void setup() {
    appConfig = new AppConfig();
  }

  @Test
  void shouldParseDvrEmail() {
    final String body = """
      This is an automatically generated e-mail from your DVR.

      EVENT TYPE:    Motion Detected
      EVENT TIME:    2025-07-27,11:41:29
      DVR NAME:      Embedded Net DVR
      DVR S/N:       RESTRICTED
      CAMERA NAME(NUM):   CAM 2(A2)
      """;

    final Map<String, Object> expected = Map.of(
      "eventType", "Motion Detected",
      "eventTime", Timestamp.valueOf(LocalDateTime.of(2025, 7, 27, 11, 41, 29)),
      "dvrName", "Embedded Net DVR",
      "dvrSn", "RESTRICTED",
      "cameraName", "CAM 2",
      "cameraNum", "A2"
    );

    performParserTest(new DvrEmailParser(appConfig), body, expected);
  }

  @Test
  void shouldParseNasEmail() {
    final String body = """
      A SMART Test Was Performed On The Following Hard Drive At 03:2:10 On 27-July-2025.

      Device Model:  WDC WD20EFZX-68AWUN0
      Serial Number:  RESTRICTED
      Size: 2,000G

      The Result Of The Test Is: Pass

      Sincerely,
      Your dlink-02C972
      """;

    final Map<String, Object> expected = Map.of(
      "model", "WDC WD20EFZX-68AWUN0",
      "serial", "RESTRICTED",
      "size", "2,000G",
      "result", "Pass",
      "eventTime", Timestamp.valueOf(LocalDateTime.of(2025, 7, 27, 3, 2, 10))
    );

    performParserTest(new NasEmailParser(appConfig), body, expected);
  }

  private void performParserTest(EmailParser parser, String body, Map<String, Object> expected) {
    final String subject = "Test " + parser.parserName() + " email";
    final EmailContent emailContent = new EmailContent(parser.senderName(), subject, body);

    final Map<String, Object> extendedMap = new HashMap<>(expected);
    extendedMap.put("subject", subject);

    final List<EmailPropertyValue> emailProperties = parser.parseEmail(emailContent);
    assertNotNull(emailProperties);

    for (final EmailPropertyValue value : emailProperties) {
      final Object expectedValue = extendedMap.get(value.name());
      assertEquals(expectedValue, value.value());
    }
  }
}
