package sporty.consumer.persistence;

public enum OutboxStatus {
  NEW,
  PROCESSING,
  RETRY,
  SENT,
  FAILED
}
