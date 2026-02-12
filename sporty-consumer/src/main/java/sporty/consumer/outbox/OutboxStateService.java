package sporty.consumer.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import sporty.consumer.persistence.OutboxEntity;
import sporty.consumer.persistence.OutboxRepository;
import sporty.consumer.persistence.OutboxStatus;

@Service
public class OutboxStateService {
  private static final Logger log = LoggerFactory.getLogger(OutboxStateService.class);

  private final OutboxRepository outboxRepository;
  private final Counter rocketmqSentCounter;
  private final Duration retryBackoff;
  private final int maxAttempts;

  public OutboxStateService(
      OutboxRepository outboxRepository,
      MeterRegistry meterRegistry,
      @Value("${app.outbox.retry-backoff-ms:5000}") long retryBackoffMs,
      @Value("${app.outbox.max-attempts:3}") int maxAttempts) {
    this.outboxRepository = outboxRepository;
    this.rocketmqSentCounter =
        Counter.builder("app.rocketmq.sent")
            .description("RocketMQ messages successfully sent")
            .register(meterRegistry);
    this.retryBackoff = Duration.ofMillis(Math.max(1000, retryBackoffMs));
    this.maxAttempts = Math.max(1, maxAttempts);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public List<OutboxEntity> claimBatch(
      List<OutboxStatus> statuses, Instant now, Instant processingTimeout, int batchSize) {
    int resetCount =
        outboxRepository.resetStaleProcessing(
            OutboxStatus.PROCESSING, OutboxStatus.RETRY, now, processingTimeout);
    if (resetCount > 0) {
      log.info("outbox reset {} stale PROCESSING records to RETRY", resetCount);
    }
    List<String> statusNames = statuses.stream().map(Enum::name).toList();
    List<OutboxEntity> items =
        outboxRepository.findReadyForUpdate(statusNames, now, maxAttempts, batchSize);
    if (items.isEmpty()) {
      return List.of();
    }
    log.info("outbox claim returned {} records", items.size());
    Instant updatedAt = Instant.now();
    for (OutboxEntity entity : items) {
      entity.setStatus(OutboxStatus.PROCESSING);
      entity.setUpdatedAt(updatedAt);
    }
    outboxRepository.saveAll(items);
    return items;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void markSent(List<OutboxEntity> batch) {
    Instant now = Instant.now();
    List<OutboxEntity> fresh = outboxRepository.findAllById(toIds(batch));
    for (OutboxEntity entity : fresh) {
      entity.setStatus(OutboxStatus.SENT);
      entity.setUpdatedAt(now);
      rocketmqSentCounter.increment();
    }
    outboxRepository.saveAll(fresh);
    log.info("outbox marked {} records as SENT", fresh.size());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void markRetryOrFailed(List<OutboxEntity> batch, Throwable e) {
    Instant now = Instant.now();
    List<OutboxEntity> fresh = outboxRepository.findAllById(toIds(batch));
    for (OutboxEntity entity : fresh) {
      int nextAttempt = entity.getAttemptCount() + 1;
      entity.setAttemptCount(nextAttempt);
      entity.setUpdatedAt(now);
      if (nextAttempt >= maxAttempts) {
        entity.setStatus(OutboxStatus.FAILED);
        entity.setNextAttemptAt(null);
      } else {
        entity.setStatus(OutboxStatus.RETRY);
        entity.setNextAttemptAt(now.plus(retryBackoff));
      }
    }
    outboxRepository.saveAll(fresh);
    log.warn("outbox send failed for {} records", fresh.size(), e);
  }

  private List<String> toIds(List<OutboxEntity> items) {
    return items.stream().map(OutboxEntity::getId).toList();
  }
}
