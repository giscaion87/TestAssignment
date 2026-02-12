package sporty.consumer.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import sporty.consumer.persistence.BetEntity;
import sporty.consumer.persistence.BetRepository;
import sporty.consumer.outbox.OutboxWriter;

@Service
public class BetSettlementPublisher {
  private final BetRepository betRepository;
  private final OutboxWriter outboxWriter;
  private final int batchSize;

  public BetSettlementPublisher(
      BetRepository betRepository,
      OutboxWriter outboxWriter,
      @Value("${app.bet.page-size:500}") int batchSize) {
    this.betRepository = betRepository;
    this.outboxWriter = outboxWriter;
    this.batchSize = Math.max(1, batchSize);
  }

  public void publishForEvent(String eventId, String eventWinnerId) {
    int pageIndex = 0;
    Page<BetEntity> page;
    do {
      page =
          betRepository.findByEventIdAndEventWinnerId(
              eventId,
              eventWinnerId,
              PageRequest.of(pageIndex, batchSize, Sort.by("betId")));
      if (page.isEmpty()) {
        return;
      }
      outboxWriter.enqueueBetSettlements(page.getContent());
      pageIndex++;
    } while (page.hasNext());
  }

}
