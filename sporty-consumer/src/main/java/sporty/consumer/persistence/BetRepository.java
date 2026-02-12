package sporty.consumer.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetRepository extends JpaRepository<BetEntity, String> {
  Page<BetEntity> findByEventIdAndEventWinnerId(
      String eventId, String eventWinnerId, Pageable pageable);
}
