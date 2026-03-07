package pl.miloszgilga.event.proxy.server.db.dao;

import pl.miloszgilga.event.proxy.server.db.dto.EventContent;
import pl.miloszgilga.event.proxy.server.db.dto.EventContentWithBody;
import pl.miloszgilga.event.proxy.server.http.EventTableSource;
import pl.miloszgilga.event.proxy.server.http.Page;
import pl.miloszgilga.event.proxy.server.queue.EmailProperties;
import pl.miloszgilga.event.proxy.server.registry.ContentInitializer;

public interface EventDao extends ContentInitializer {
  Page<EventContent> getAllByOptionalEventSource(
    EventTableSource tableSource,
    String eventSource,
    String subjectSearch,
    boolean isAscending,
    int limit,
    int offset
  );

  EventContentWithBody getSingleById(EventTableSource tableSource, long id);

  boolean eventSourceExists(String eventSource);

  boolean makeEventRead(EventTableSource tableSource, long id);

  // executed with blocking mode, returns persisted id
  long persist(String eventSource, EmailProperties emailProperties);

  boolean archiveAllByOptionalEventSource(String eventSource);

  boolean archiveMultipleByIds(long[] ids);

  boolean unarchiveAllByOptionalEventSource(String eventSource);

  boolean unarchiveMultipleByIds(long[] ids);

  boolean deleteAllByOptionalEventSource(EventTableSource tableSource, String eventSource);

  boolean deleteMultipleByIds(EventTableSource tableSource, long[] ids);
}
