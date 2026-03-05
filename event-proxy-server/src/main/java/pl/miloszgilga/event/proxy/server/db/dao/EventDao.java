package pl.miloszgilga.event.proxy.server.db.dao;

import pl.miloszgilga.event.proxy.server.db.dto.EventContent;
import pl.miloszgilga.event.proxy.server.db.dto.EventContentWithBody;
import pl.miloszgilga.event.proxy.server.http.Page;
import pl.miloszgilga.event.proxy.server.queue.EmailProperties;
import pl.miloszgilga.event.proxy.server.registry.ContentInitializer;

public interface EventDao extends ContentInitializer {
  Page<EventContent> getAllByEventSource(String eventSource, int limit, int offset);

  EventContentWithBody getSingleById(long id);

  boolean eventSourceExists(String eventSource);

  boolean makeEventRead(long id);

  // executed with blocking mode, returns persisted id
  long persist(String eventSource, EmailProperties emailProperties);

  boolean deleteAllByEventSource(String eventSource);

  boolean deleteMultipleByIds(long[] id);
}
