package pl.miloszgilga.event.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class AbstractEmailParser implements EmailParser {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractEmailParser.class);

  private static final String SENDER_SUFFIX = "event-proxy-system";
  private static final String SENDER_SEPARATOR = "@";

  protected String extractGroup(String text, Pattern pattern, int groupIndex) {
    final Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return matcher.group(groupIndex).trim();
    }
    throw new IllegalArgumentException("Unable to find pattern for: " + pattern.pattern());
  }

  @Override
  public final Map<String, EmailPropertyValue> parseEmail(EmailContent emailContent) {
    final String rawBody = emailContent.rawBody();
    final Map<String, EmailPropertyValue> values = new HashMap<>();
    try {
      parseWithExceptionWrapper(rawBody, values);
      values.put("subject", new EmailPropertyValue(emailContent.subject()));
    } catch (Exception ex) {
      LOG.error("Unable to parse {} message. Cause: {}", parserName(), ex.getMessage());
    }
    return values;
  }

  @Override
  public final String senderName() {
    return parserName() + SENDER_SEPARATOR + SENDER_SUFFIX;
  }

  protected abstract void parseWithExceptionWrapper(String rawBody,
                                                    Map<String, EmailPropertyValue> values);
}
