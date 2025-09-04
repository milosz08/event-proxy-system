package pl.miloszgilga.event.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

class EmailConsumer extends AbstractThread {
  private final Logger LOG = LoggerFactory.getLogger(EmailConsumer.class);

  private final BlockingQueue<EmailContent> queue;
  private final Map<String, EmailParser> emailParsers;
  private final EventBroadcaster eventBroadcaster;
  private final EmailPersistor emailPersistor;

  EmailConsumer(BlockingQueue<EmailContent> queue, List<EmailParser> emailParsers,
                EventBroadcaster eventBroadcaster, EmailPersistor emailPersistor) {
    super("Email-Consumer");
    this.queue = queue;
    // put email parsers to map for increase speed while search parser instance
    this.emailParsers = emailParsers.stream()
      .collect(Collectors.toMap(EmailParser::senderName, Function.identity()));
    this.eventBroadcaster = eventBroadcaster;
    this.emailPersistor = emailPersistor;
  }

  @Override
  public void run() {
    while (running) {
      try {
        final EmailContent emailContent = queue.take();
        final EmailParser emailParser = emailParsers.get(emailContent.from());
        if (emailParser != null) {
          final EmailPropertiesAggregator parsedEmail = emailParser.parseEmail(emailContent);
          if (parsedEmail != null) {
            eventBroadcaster.broadcastEvent(emailParser.parserName(), parsedEmail);
            emailPersistor.persist(emailParser.parserName(), parsedEmail);
          }
        }
      } catch (InterruptedException ignored) {
      }
    }
  }

  @Override
  void beforeStopThread() {
    LOG.info("Email consumer thread has been stopped");
  }
}
