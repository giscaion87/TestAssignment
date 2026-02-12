# Sporty Rest Gateway

Spring Cloud Gateway (WebFlux) for routing requests to the Sporty API with a global request-size cap.

## Run

From this directory:

```bash
mvn spring-boot:run
```

Gateway listens on port 8080 and forwards to the API at `http://localhost:8081`.

## Config

`application.properties`:

- `server.port=8080`
- `spring.threads.virtual.enabled=true`
- `spring.cloud.gateway.default-filters[0]=RequestSize=10KB`
- `spring.cloud.gateway.routes[0].uri=http://localhost:8081`
- `spring.cloud.gateway.routes[0].predicates[0]=Path=/**`

Adjust the upstream URI as needed when moving to your internal URL.
