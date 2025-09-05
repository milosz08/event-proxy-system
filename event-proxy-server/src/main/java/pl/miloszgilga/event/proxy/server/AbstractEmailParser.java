package pl.miloszgilga.event.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class AbstractEmailParser implements EmailParser {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractEmailParser.class);
  private static final String SENDER_SEPARATOR = "@";

  private final Map<String, FieldType> parserFields;
  private final String senderSuffix;

  AbstractEmailParser(AppConfig appConfig) {
    parserFields = declareParserFields();
    senderSuffix = appConfig.getAsStr(AppConfig.Prop.SMTP_SENDER_SUFFIX);
  }

  protected String extractGroup(String text, Pattern pattern, int groupIndex) {
    final Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return matcher.group(groupIndex).trim();
    }
    throw new IllegalArgumentException("Unable to find pattern for: " + pattern.pattern());
  }

  @Override
  public final Map<String, FieldType> declareParserFields() {
    final Map<String, FieldType> parserFields = new HashMap<>();
    parserFields.put("subject", FieldType.TEXT);
    parserFields.put("eventTime", FieldType.TEXT);
    declareOwnParserFields(parserFields);
    return parserFields;
  }

  @Override
  public final List<EmailPropertyValue> parseEmail(EmailContent emailContent) {
    final String rawBody = emailContent.rawBody();
    final Map<String, Object> values = new HashMap<>();
    try {
      values.put("subject", emailContent.subject());
      final LocalDateTime eventTime = parseWithExceptionWrapper(rawBody, values);
      if (!values.containsKey("eventTime")) {
        values.put("eventTime", eventTime.toString());
      }
      // if parser fields map has different keys compared to values map, throw exception
      if (!parserFields.keySet().equals(values.keySet())) {
        throw new IllegalStateException("Values map has different keys compare to parser map.");
      }
      // get declared parser fields and map into EmailPropertyValue
      // keys has been checked before
      return values.entrySet().stream()
        // map must be parsed to list (elements must be in defined order for SQL statements)
        .map(entry -> new EmailPropertyValue(entry.getKey(), entry.getValue(),
          parserFields.get(entry.getKey())))
        .toList();
    } catch (Exception ex) {
      LOG.error("Unable to parse {} message. Cause: {}", parserName(), ex.getMessage());
    }
    return null;
  }

  @Override
  public final String senderName() {
    return parserName() + SENDER_SEPARATOR + senderSuffix;
  }

  protected abstract void declareOwnParserFields(Map<String, FieldType> values);

  // return event time as LocalDateTime
  protected abstract LocalDateTime parseWithExceptionWrapper(String rawBody, Map<String,
    Object> values);
}
