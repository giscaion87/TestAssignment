package sporty.consumer.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import sporty.consumer.persistence.OutboxEntity;
import sporty.consumer.persistence.OutboxStatus;

@Service
public class OutboxDispatcher {

  private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

  private final RocketMQTemplate rocketMQTemplate;
  private final OutboxStateService outboxStateService;
  private final String topic;
  private final int batchSize;
  private final Duration processingTimeout;
  private final Duration tickTimeBudget;
  private final int maxBatchBytes;
  private final int messageOverheadBytes;

  public OutboxDispatcher(
      RocketMQTemplate rocketMQTemplate,
      OutboxStateService outboxStateService,
      @Value("${app.rocketmq.topic:bet-settlements}") String topic,
      @Value("${app.outbox.batch-size:50}") int batchSize,
      @Value("${app.outbox.processing-timeout-ms:30000}") long processingTimeoutMs,
      @Value("${app.outbox.tick-budget-ms:15000}") long tickBudgetMs,
      @Value("${app.outbox.max-batch-bytes:1048576}") int maxBatchBytes,
      @Value("${app.outbox.message-overhead-bytes:64}") int messageOverheadBytes) {
    this.rocketMQTemplate = rocketMQTemplate;
    this.outboxStateService = outboxStateService;
    this.topic = topic;
    this.batchSize = Math.max(1, batchSize);
    this.processingTimeout = Duration.ofMillis(Math.max(1000, processingTimeoutMs));
    this.tickTimeBudget = Duration.ofMillis(Math.max(100, tickBudgetMs));
    this.maxBatchBytes = Math.max(1024, maxBatchBytes);
    this.messageOverheadBytes = Math.max(0, messageOverheadBytes);
  }

  @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
  public void dispatch() {
    Instant deadline = Instant.now().plus(tickTimeBudget);
    while (Instant.now().isBefore(deadline)) {
      List<OutboxEntity> items =
          outboxStateService.claimBatch(
              Arrays.asList(OutboxStatus.NEW, OutboxStatus.RETRY),
              Instant.now(),
              Instant.now().minus(processingTimeout),
              batchSize);
      if (items.isEmpty()) {
        return;
      }
      log.info("outbox dispatch sending batch with {} records", items.size());
      sendBatch(items);
    }
  }

  private void sendBatch(List<OutboxEntity> batch) {
    for (List<OutboxEntity> chunk : splitByBytes(batch)) {
      sendRocketBatch(chunk);
    }
  }

  private List<List<OutboxEntity>> splitByBytes(List<OutboxEntity> batch) {
    List<List<OutboxEntity>> result = new ArrayList<>();
    List<OutboxEntity> current = new ArrayList<>();
    int currentBytes = 0;
    for (OutboxEntity entity : batch) {
      int size = estimateMessageSize(entity);
      if (size > maxBatchBytes) {
        if (!current.isEmpty()) {
          result.add(current);
          current = new ArrayList<>();
          currentBytes = 0;
        }
        result.add(List.of(entity));
        continue;
      }
      if (!current.isEmpty() && currentBytes + size > maxBatchBytes) {
        result.add(current);
        current = new ArrayList<>();
        currentBytes = 0;
      }
      current.add(entity);
      currentBytes += size;
    }
    if (!current.isEmpty()) {
      result.add(current);
    }
    return result;
  }

  private int estimateMessageSize(OutboxEntity entity) {
    int payloadSize = entity.getPayload() == null ? 0 : entity.getPayload().length;
    return payloadSize + messageOverheadBytes;
  }

  private void sendRocketBatch(List<OutboxEntity> batch) {
    List<Message> messages = new ArrayList<>(batch.size());
    for (OutboxEntity entity : batch) {
      messages.add(new Message(topic, entity.getPayload()));
    }
    try {
      rocketMQTemplate.getProducer().send(messages, new OutboxSendCallback(batch));
    } catch (Exception e) {
      outboxStateService.markRetryOrFailed(batch, e);
    }
  }

  private class OutboxSendCallback implements SendCallback {
    private final List<OutboxEntity> batch;

    private OutboxSendCallback(List<OutboxEntity> batch) {
      this.batch = batch;
    }

    @Override
    public void onSuccess(SendResult sendResult) {
      outboxStateService.markSent(batch);
    }

    @Override
    public void onException(Throwable e) {
      outboxStateService.markRetryOrFailed(batch, e);
    }
  }
}
