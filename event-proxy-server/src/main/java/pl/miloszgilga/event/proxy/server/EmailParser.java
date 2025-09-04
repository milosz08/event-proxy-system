package pl.miloszgilga.event.proxy.server;

import java.util.Map;

interface EmailParser {
  String parserName();

  Map<String, FieldType> declareParserFields();

  String senderName();

  EmailPropertiesAggregator parseEmail(EmailContent emailContent);
}
