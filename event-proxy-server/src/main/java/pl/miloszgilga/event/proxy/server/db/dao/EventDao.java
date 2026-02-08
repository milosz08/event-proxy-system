package pl.miloszgilga.event.proxy.server.db.dao;

import pl.miloszgilga.event.proxy.server.db.dto.MessageContent;
import pl.miloszgilga.event.proxy.server.db.dto.MessageContentWithBody;
import pl.miloszgilga.event.proxy.server.http.Page;
import pl.miloszgilga.event.proxy.server.queue.EmailProperties;
import pl.miloszgilga.event.proxy.server.registry.ContentInitializer;

import java.util.List;

public interface EventDao extends ContentInitializer {
  List<String> getEventSources();

  Page<MessageContent> getAllByEventSource(String eventSource, int limit, int offset);

  MessageContentWithBody getSingleById(long id);

  boolean eventSourceExists(String eventSource);

  // executed with blocking mode
  void persist(String eventSource, EmailProperties emailProperties);

  boolean deleteAllByEventSource(String eventSource);

  boolean deleteSingleById(long id);
}
