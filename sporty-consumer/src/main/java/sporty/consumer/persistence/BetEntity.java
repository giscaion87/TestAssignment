package sporty.consumer.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bet")
public class BetEntity {
  @Id
  @Column(name = "bet_id", nullable = false, length = 64)
  private String betId;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  @Column(name = "event_id", nullable = false, length = 64)
  private String eventId;

  @Column(name = "event_market_id", nullable = false, length = 64)
  private String eventMarketId;

  @Column(name = "event_winner_id", nullable = false, length = 64)
  private String eventWinnerId;

  @Column(name = "bet_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal betAmount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public String getBetId() {
    return betId;
  }

  public void setBetId(String betId) {
    this.betId = betId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getEventMarketId() {
    return eventMarketId;
  }

  public void setEventMarketId(String eventMarketId) {
    this.eventMarketId = eventMarketId;
  }

  public String getEventWinnerId() {
    return eventWinnerId;
  }

  public void setEventWinnerId(String eventWinnerId) {
    this.eventWinnerId = eventWinnerId;
  }

  public BigDecimal getBetAmount() {
    return betAmount;
  }

  public void setBetAmount(BigDecimal betAmount) {
    this.betAmount = betAmount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
