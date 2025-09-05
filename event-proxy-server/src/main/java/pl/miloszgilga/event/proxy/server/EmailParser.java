package pl.miloszgilga.event.proxy.server;

import java.util.List;
import java.util.Map;

interface EmailParser {
  String parserName();

  Map<String, FieldType> declareParserFields();

  String senderName();

  List<EmailPropertyValue> parseEmail(EmailContent emailContent);
}
