# Sporty Rest Producer API

Spring Boot REST API for publishing events.

## Run

From this directory:

```bash
mvn spring-boot:run
```

API listens on port 8081 by default.

## Endpoint

`POST /publish`

Request JSON:
```json
{
  "event_id": "e1",
  "event_name": "Final",
  "event_winner_id": "w9"
}
```

Response JSON (202 Accepted):
```json
{
  "status": "accepted",
  "event_id": "e1"
}
```

## Config

`application.properties`:

- `spring.threads.virtual.enabled=true`
- `server.port=8081`
- `app.publish.max-in-flight` (default 500 in code)
- `app.publish.max-request-bytes` (default 2048 in code)
- `server.tomcat.max-http-post-size=2048`
- `app.kafka.topic.name=event-outcomes` (default in code)

## Validation

All request fields are required (`@NotBlank`).
