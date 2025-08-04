package pl.miloszgilga.event.proxy.server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

class NasEmailParser extends AbstractEmailParser {
  private static final Pattern TIMESTAMP_PATTERN = Pattern
    .compile("At (\\d{1,2}:\\d{1,2}:\\d{1,2}) On (\\d{1,2}-[A-Za-z]+-\\d{4})");
  private static final Pattern MODEL_PATTERN = Pattern.compile("Device Model:\\s*(.*)");
  private static final Pattern SERIAL_PATTERN = Pattern.compile("Serial Number:\\s*(\\S+)");
  private static final Pattern SIZE_PATTERN = Pattern.compile("Size:\\s*(.*)");
  private static final Pattern RESULT_PATTERN = Pattern
    .compile("The Result Of The Test Is:\\s*(\\w+)");

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
    .ofPattern("H:m:s d-MMMM-yyyy", Locale.ENGLISH);

  @Override
  public String parserName() {
    return "nas";
  }

  @Override
  protected void declareOwnParserFields(Map<String, FieldType> values) {
    values.put("model", FieldType.TEXT);
    values.put("serial", FieldType.TEXT);
    values.put("size", FieldType.TEXT);
    values.put("result", FieldType.TEXT);
    values.put("timestamp", FieldType.TEXT);
  }

  @Override
  protected void parseWithExceptionWrapper(String rawBody, Map<String, Object> values) {
    final String timePart = extractGroup(rawBody, TIMESTAMP_PATTERN, 1);
    final String datePart = extractGroup(rawBody, TIMESTAMP_PATTERN, 2);

    final String model = extractGroup(rawBody, MODEL_PATTERN, 1);
    final String serial = extractGroup(rawBody, SERIAL_PATTERN, 1);
    final String size = extractGroup(rawBody, SIZE_PATTERN, 1);
    final String result = extractGroup(rawBody, RESULT_PATTERN, 1);

    final LocalDateTime timestamp = LocalDateTime
      .parse(timePart + " " + datePart, TIME_FORMATTER);

    values.put("model", model);
    values.put("serial", serial);
    values.put("size", size);
    values.put("result", result);
    values.put("timestamp", timestamp.toString());
  }
}
