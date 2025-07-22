package pl.miloszgilga.event.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

class EmailConsumer extends AbstractThread {
  private final Logger LOG = LoggerFactory.getLogger(EmailConsumer.class);

  private final BlockingQueue<EmailContent> queue;

  EmailConsumer(BlockingQueue<EmailContent> queue) {
    super("Email-Consumer");
    this.queue = queue;
  }

  @Override
  public void run() {
    while (running) {
      try {
        final EmailContent emailContent = queue.take();
        System.out.println(emailContent);

        // TODO: create class for parsing email template
        // TODO: save parsed email into sqlite database

      } catch (InterruptedException ignored) {
      }
    }
  }

  @Override
  void beforeStopThread() {
    LOG.info("Email consumer thread has been stopped");
  }
}
