package sporty.consumer.outbox;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import sporty.consumer.persistence.BetEntity;
import sporty.consumer.persistence.OutboxEntity;
import sporty.consumer.persistence.OutboxRepository;
import sporty.consumer.persistence.OutboxStatus;
import sporty.consumer.proto.BetSettlement;

@Service
public class OutboxWriter {
  private final OutboxRepository outboxRepository;

  public OutboxWriter(OutboxRepository outboxRepository) {
    this.outboxRepository = outboxRepository;
  }

  public void enqueueBetSettlements(List<BetEntity> bets) {
    if (bets.isEmpty()) {
      return;
    }
    Instant now = Instant.now();
    List<OutboxEntity> entities = new ArrayList<>(bets.size());
    List<String> ids = new ArrayList<>(bets.size());
    for (BetEntity bet : bets) {
      ids.add(bet.getBetId());
    }
    Set<String> existing = new HashSet<>(outboxRepository.findExistingIds(ids));
    for (BetEntity bet : bets) {
      if (existing.contains(bet.getBetId())) {
        continue;
      }
      BetSettlement payload =
          BetSettlement.newBuilder()
              .setBetId(bet.getBetId())
              .setUserId(bet.getUserId())
              .setAmount(bet.getBetAmount().stripTrailingZeros().toPlainString())
              .build();

      OutboxEntity entity = new OutboxEntity();
      entity.setId(bet.getBetId());
      entity.setPayload(payload.toByteArray());
      entity.setStatus(OutboxStatus.NEW);
      entity.setAttemptCount(0);
      entity.setNextAttemptAt(null);
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);
      entities.add(entity);
    }
    if (!entities.isEmpty()) {
      outboxRepository.saveAll(entities);
    }
  }
}
