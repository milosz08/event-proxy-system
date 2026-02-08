package pl.miloszgilga.event.proxy.server.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.AbstractThread;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.http.sse.EventBroadcaster;
import pl.miloszgilga.event.proxy.server.smtp.EmailContent;

import java.util.concurrent.BlockingQueue;

public class EmailConsumer extends AbstractThread {
  private final Logger LOG = LoggerFactory.getLogger(EmailConsumer.class);

  private final BlockingQueue<EmailContent> queue;
  private final EventBroadcaster eventBroadcaster;
  private final EventDao eventDao;

  public EmailConsumer(BlockingQueue<EmailContent> queue, EventBroadcaster eventBroadcaster,
                       EventDao eventDao) {
    super("Email-Consumer");
    this.queue = queue;
    this.eventBroadcaster = eventBroadcaster;
    this.eventDao = eventDao;
  }

  @Override
  public void run() {
    while (running) {
      try {
        final EmailContent emailContent = queue.take();

        final EmailProperties properties = new EmailProperties(
          emailContent.subject(),
          emailContent.rawBody(),
          emailContent.receivedAt()
        );
        final long id = eventDao.persist(emailContent.from(), properties);
        eventBroadcaster.broadcastEvent(emailContent.from(), id, properties);
      } catch (InterruptedException ignored) {
      }
    }
  }

  @Override
  protected void beforeStopThread() {
    LOG.info("Email consumer thread has been stopped");
  }
}
