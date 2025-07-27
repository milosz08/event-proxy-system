package pl.miloszgilga.event.proxy.server;

import java.sql.JDBCType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Pattern;

class DvrEmailParser extends AbstractEmailParser {
  private static final Pattern EVENT_TYPE_PATTERN = Pattern.compile("EVENT TYPE:\\s*(.*)");
  private static final Pattern EVENT_TIME_PATTERN = Pattern.compile("EVENT TIME:\\s*(.*)");
  private static final Pattern DVR_NAME_PATTERN = Pattern.compile("DVR NAME:\\s*(.*)");
  private static final Pattern DVR_SN_PATTERN = Pattern.compile("DVR S/N:\\s*(\\S+)");
  private static final Pattern CAMERA_PATTERN = Pattern
    .compile("CAMERA NAME\\(NUM\\):\\s*([^(]+)\\(([^)]+)\\)");

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
    .ofPattern("yyyy-MM-dd,HH:mm:ss");

  @Override
  public String parserName() {
    return "dvr";
  }

  @Override
  protected void parseWithExceptionWrapper(String rawBody, Map<String, EmailPropertyValue> values) {
    final String eventType = extractGroup(rawBody, EVENT_TYPE_PATTERN, 1);
    final String eventTimeString = extractGroup(rawBody, EVENT_TIME_PATTERN, 1);
    final String dvrName = extractGroup(rawBody, DVR_NAME_PATTERN, 1);
    final String dvrSn = extractGroup(rawBody, DVR_SN_PATTERN, 1);
    final String cameraName = extractGroup(rawBody, CAMERA_PATTERN, 1);
    final String cameraNum = extractGroup(rawBody, CAMERA_PATTERN, 2);

    final LocalDateTime eventTime = LocalDateTime.parse(eventTimeString, TIME_FORMATTER);

    values.put("eventType", new EmailPropertyValue(eventType));
    values.put("eventTime", new EmailPropertyValue(eventTime, JDBCType.TIMESTAMP));
    values.put("dvrName", new EmailPropertyValue(dvrName));
    values.put("dvrSn", new EmailPropertyValue(dvrSn));
    values.put("cameraName", new EmailPropertyValue(cameraName));
    values.put("cameraNum", new EmailPropertyValue(cameraNum));
  }
}
