package pl.miloszgilga.event.proxy.server.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.AbstractThread;
import pl.miloszgilga.event.proxy.server.db.dao.EventDao;
import pl.miloszgilga.event.proxy.server.http.sse.EventBroadcaster;
import pl.miloszgilga.event.proxy.server.parser.EmailContent;
import pl.miloszgilga.event.proxy.server.parser.EmailParser;
import pl.miloszgilga.event.proxy.server.parser.EmailPropertyValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EmailConsumer extends AbstractThread {
  private final Logger LOG = LoggerFactory.getLogger(EmailConsumer.class);

  private final BlockingQueue<EmailContent> queue;
  private final Map<String, EmailParser> emailParsers;
  private final EventBroadcaster eventBroadcaster;
  private final EventDao eventDao;

  public EmailConsumer(BlockingQueue<EmailContent> queue, List<EmailParser> emailParsers,
                       EventBroadcaster eventBroadcaster, EventDao eventDao) {
    super("Email-Consumer");
    this.queue = queue;
    // put email parsers to map for increase speed while search parser instance
    this.emailParsers = emailParsers.stream()
      .collect(Collectors.toMap(EmailParser::senderName, Function.identity()));
    this.eventBroadcaster = eventBroadcaster;
    this.eventDao = eventDao;
  }

  @Override
  public void run() {
    while (running) {
      try {
        final EmailContent emailContent = queue.take();
        final EmailParser emailParser = emailParsers.get(emailContent.from());
        if (emailParser != null) {
          final List<EmailPropertyValue> emailProperties = emailParser.parseEmail(emailContent);
          if (emailProperties != null) {
            eventBroadcaster.broadcastEvent(emailParser.parserName(), emailProperties);
            eventDao.persist(emailParser.parserName(), emailProperties);
          }
        }
      } catch (InterruptedException ignored) {
      }
    }
  }

  @Override
  protected void beforeStopThread() {
    LOG.info("Email consumer thread has been stopped");
  }
}
