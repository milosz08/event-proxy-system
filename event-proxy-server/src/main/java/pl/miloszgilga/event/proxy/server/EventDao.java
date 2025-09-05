package pl.miloszgilga.event.proxy.server;

import java.util.List;
import java.util.Map;

interface EventDao extends ContentInitializer {
  Page<Map<String, Object>> getAllByEventSource(String eventSource, int limit, int offset);

  Map<String, Object> getSingleById(String eventSource, long id);

  // executed with blocking mode
  void persist(String eventSource, List<EmailPropertyValue> emailProperties);

  void deleteAllByEventSource(String eventSource);

  void deleteSingleById(String eventSource, long id);
}
