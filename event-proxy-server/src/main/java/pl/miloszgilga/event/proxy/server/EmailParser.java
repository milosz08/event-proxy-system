package pl.miloszgilga.event.proxy.server;

import java.util.Map;

interface EmailParser {
  String parserName();

  String senderName();

  Map<String, EmailPropertyValue> parseEmail(EmailContent emailContent);
}
