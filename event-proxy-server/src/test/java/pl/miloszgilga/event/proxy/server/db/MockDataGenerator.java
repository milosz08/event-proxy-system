package pl.miloszgilga.event.proxy.server.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.miloszgilga.event.proxy.server.TestData;
import pl.miloszgilga.event.proxy.server.TestDataPayload;
import pl.miloszgilga.event.proxy.server.queue.EmailProperties;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MockDataGenerator extends MockData {
  private static final Logger LOG = LoggerFactory.getLogger(MockDataGenerator.class);
  private static final int RECORDS_TO_GENERATE = 1000;
  private static final int CHANCE_TO_PUT_IN_ARCHIVE = 30; // %

  private final Random random = new Random();

  public static void main(String[] args) {
    final MockDataGenerator mockDataGenerator = new MockDataGenerator();
    mockDataGenerator.generate();
  }

  private void generate() {
    LOG.info("Starting mock data generation with {} suffix...", MOCK_DATA_SUFFIX);

    final Instant now = Instant.now();
    final List<Long> idsToArchive = new ArrayList<>();

    for (int i = 0; i < RECORDS_TO_GENERATE; i++) {
      final String source, subject, body;
      switch (random.nextInt(3)) {
        case 0 -> {
          final TestDataPayload payload = TestData.NAS.apply(MOCK_DATA_SUFFIX);
          source = payload.from();
          subject = payload.subject();
          body = payload.rawBody();
        }
        case 1 -> {
          final String chan = String.valueOf(random.nextInt(4) + 1);
          final TestDataPayload payload = TestData.DVR.apply(chan, MOCK_DATA_SUFFIX);
          source = payload.from();
          subject = payload.subject();
          body = payload.rawBody();
        }
        default -> {
          source = parseSourceName("SYSTEM");
          subject = "SYSTEM: Node status check";
          body = "System check performed on node " + random.nextInt(100) + ". " +
            "All services operational.";
        }
      }
      final Instant eventTime = now
        .minus(i, ChronoUnit.MINUTES)
        .minusSeconds(random.nextInt(60));
      final EmailProperties properties = new EmailProperties(subject, body, eventTime);

      final long generatedId = eventDao.persist(source, properties);
      if (generatedId != -1 && random.nextInt(100) < CHANCE_TO_PUT_IN_ARCHIVE) {
        idsToArchive.add(generatedId);
      }
      if (i % 200 == 0) {
        LOG.info("Generated {} of {} records...", i, RECORDS_TO_GENERATE);
      }
    }
    if (!idsToArchive.isEmpty()) {
      LOG.info("Moving {} records to archive in one batch...", idsToArchive.size());
      final long[] idsArray = idsToArchive.stream().mapToLong(Long::longValue).toArray();
      eventDao.archiveMultipleByIds(idsArray);
    }
    final int archivedCount = idsToArchive.size();
    LOG.info("Finished. Total: {}, archived: {}, current: {}", RECORDS_TO_GENERATE, archivedCount,
      (RECORDS_TO_GENERATE - archivedCount));
  }
}
