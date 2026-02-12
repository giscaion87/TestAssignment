package sporty.consumer.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEntity, String> {
  @Query(
      value =
          "select * from outbox "
              + "where status in (:statuses) "
              + "and (next_attempt_at is null or next_attempt_at <= :now) "
              + "and attempt_count < :maxAttempts "
              + "order by created_at asc "
              + "limit :batchSize",
      nativeQuery = true)
  List<OutboxEntity> findReadyForUpdate(
      @Param("statuses") List<String> statuses,
      @Param("now") Instant now,
      @Param("maxAttempts") int maxAttempts,
      @Param("batchSize") int batchSize);

  @Modifying
  @Query(
      "update OutboxEntity o "
          + "set o.status = :retryStatus, o.nextAttemptAt = :now, o.updatedAt = :now "
          + "where o.status = :processingStatus and o.updatedAt <= :processingTimeout")
  int resetStaleProcessing(
      @Param("processingStatus") OutboxStatus processingStatus,
      @Param("retryStatus") OutboxStatus retryStatus,
      @Param("now") Instant now,
      @Param("processingTimeout") Instant processingTimeout);

  @Query("select o.id from OutboxEntity o where o.id in :ids")
  List<String> findExistingIds(@Param("ids") Collection<String> ids);
}
