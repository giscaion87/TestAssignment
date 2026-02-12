# Sporty Consumer

Kafka consumer for Sporty events (Protobuf).

## Run

From this directory:

```bash
mvn spring-boot:run
```

## Config

`application.properties`:

- `spring.kafka.consumer.group-id=event-settlement`
- `spring.kafka.consumer.auto-offset-reset=earliest`
- `spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer`
- `app.kafka.topic=event-outcomes` (default in `@KafkaListener`)
- `app.rocketmq.topic=bet-settlements` (default in code)
- `app.outbox.*` defaults are defined in code
- `management.endpoints.web.base-path=/internal`

## Behavior

Consumes Protobuf `Event` messages and logs with `request_trace_id` when present (falls back to legacy `event_trace_id`).
Uses retry topics (non-blocking) with a DLT:
- `event-outcomes_retry_0`, `event-outcomes_retry_1`, ...
- `event-outcomes_dlt`
