package pl.miloszgilga.event.proxy.server.db.dao;

import pl.miloszgilga.event.proxy.server.db.dto.MessageContent;
import pl.miloszgilga.event.proxy.server.db.dto.MessageContentWithBody;
import pl.miloszgilga.event.proxy.server.http.Page;
import pl.miloszgilga.event.proxy.server.queue.EmailProperties;
import pl.miloszgilga.event.proxy.server.registry.ContentInitializer;

public interface EventDao extends ContentInitializer {
  List<String> getEventSources();

  EventContentWithBody getSingleById(long id);

  boolean eventSourceExists(String eventSource);

  boolean makeEventRead(long id);

  // executed with blocking mode, returns persisted id
  long persist(String eventSource, EmailProperties emailProperties);

  boolean deleteAllByEventSource(String eventSource);

  boolean deleteSingleById(long id);
}
