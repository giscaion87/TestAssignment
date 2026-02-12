# Sporty Stack

This repository contains the Sporty test stack (gateway, producer, consumer) plus Kubernetes manifests for the `sporty` namespace.

## Architecture

Request flow (happy path):

1. Client sends `POST /publish` to the gateway.
2. `sporty-rest-gateway` (WebFlux) adds `X-Request-Trace-Id`, enforces request size, and forwards.
3. `sporty-rest-producer` validates the payload and publishes a Protobuf event to Kafka.
4. Kafka topic: `event-outcomes`.
5. `sporty-consumer` consumes events, queries H2, and writes an outbox record per bet.
6. Outbox dispatcher sends messages to RocketMQ topic `bet-settlements`.

## Deploy (local Docker Desktop k8s)

```bash
/Volumes/Work/Sporty/k8s/deploy-local.sh
```

Notes:
- Kafka is managed by Strimzi (operator must already be installed).
- RocketMQ is installed via Helm using the vendored chart under `k8s/rocketmq-helm/rocketmq`.
- HPA is pinned to 1 replica for the consumer due to H2 in-memory DB.

## Entry point (ingress)

Ingress is the default entry point for local testing.

If your ingress uses a host (see `k8s/ingress.yaml`), add to `/etc/hosts`:

```
127.0.0.1 api.test.com
```

Prepare and test (commands):

```bash
# Deploy everything
/Volumes/Work/Sporty/k8s/deploy-local.sh

# Map ingress host (only needed once)
sudo sh -c 'echo "127.0.0.1 api.test.com" >> /etc/hosts'

# Verify ingress is reachable (Docker Desktop usually exposes on port 80)
curl -i http://api.test.com/internal/health
```

Manual test (replace the token if you change it):

```bash
TOKEN="$(grep '^app.jwt.test-token=' /Volumes/Work/Sporty/sporty-rest-gateway/src/main/resources/application.properties | cut -d= -f2)"

curl -i \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"event_id":"e1","event_name":"Final","event_winner_id":"w1"}' \
  http://api.test.com/publish
```

Expected response:
- HTTP `202`
- Body `{ "status": "accepted", "event_id": "e1" }`

If you want to keep using ingress but on port 8080:

```bash
kubectl -n ingress-nginx port-forward svc/ingress-nginx-controller 8080:80
curl -i -H "Host: api.test.com" http://localhost:8080/internal/health
```

## Load test (wrk + Lua)

The Lua script is located at:
- `/Volumes/Work/Sporty/test-performance.lua`

Prereqs:
- `wrk` (includes LuaJIT, no separate Lua install needed)

Install `wrk`:

```bash
brew install wrk
```

```bash
sudo apt-get install -y wrk
```

Run:

```bash
TOKEN="$(grep '^app.jwt.test-token=' /Volumes/Work/Sporty/sporty-rest-gateway/src/main/resources/application.properties | cut -d= -f2)"

JWT_TOKEN="${TOKEN}" \
wrk -t4 -c100 -d30s \
  --timeout 10s \
  -s /Volumes/Work/Sporty/test-performance.lua \
  http://api.test.com
```

Notes:
- The script reads the JWT from the `JWT_TOKEN` env var.
- If you change request size limits, ensure the payload in the script still fits.

## Optional direct access (port-forward)

If you want to bypass ingress:

```bash
kubectl -n sporty port-forward svc/sporty-rest-gateway 8080:8080
```

If you want to keep using ingress but on port 8080:

```bash
kubectl -n ingress-nginx port-forward svc/ingress-nginx-controller 8080:80
curl -i -H "Host: api.test.com" http://localhost:8080/internal/health
```

## Metrics

- Gateway: `http://localhost:8080/internal/prometheus` (via gateway or direct service)
- Producer: `http://localhost:8081/internal/prometheus` (if port-forwarded to its service)
- Consumer: `http://localhost:8082/internal/prometheus` (if port-forwarded to its pod)
- Prometheus/Grafana are in `k8s/monitoring-*.yaml` (optional)

## Common kubectl helpers

```bash
kubectl -n sporty get pods
kubectl -n sporty get svc
kubectl -n sporty logs deploy/sporty-rest-producer --tail=100
kubectl -n sporty logs deploy/sporty-consumer --tail=100
```
