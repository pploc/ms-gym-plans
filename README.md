# ms-gym-plans

Plans catalog service for gym locations and membership plans.

## Scope (G7 / V1)

- Owns `plans_db` tables `gym_locations` and `membership_plans`
- Public gRPC + HTTP gateway routes for catalog CRUD/list
- Internal mTLS-only RPCs:
  - `GetActiveGym` — Identifier only
  - `ResolvePurchasablePlan` — Member only
- No Kafka, outbox, cache, scheduler, Redis, or Payment client

## Dependencies

- Java 26, Spring Boot 4.1
- `com.gym:common-java:2.0.0`
- `com.gym.proto:gym-proto-java:3.0.0`
- PostgreSQL (`plans_db`)

## Run locally

```bash
# token for GitHub Packages
export GITHUB_TOKEN=...
export GITHUB_ACTOR=pploc

docker compose up -d
./gradlew bootRun
```

Gateway (separate process):

```bash
cd gateway
go test ./...
HTTP_ADDR=:8080 PLANS_GRPC_ADDR=127.0.0.1:50051 go run ./cmd/server
```

## Tests

```bash
./gradlew clean check
cd gateway && go test ./...
```

## Config

| Env | Default | Purpose |
|-----|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/plans_db` | DB |
| `GRPC_SERVER_PORT` | `50051` | Plans gRPC |
| `PLANS_GRPC_TLS_ENABLED` | `true` | mTLS |
| `PLANS_GRPC_ALLOW_PLAINTEXT` | `false` | test-only plaintext |
| `PLANS_GRPC_SERVER_CERT` / `KEY` / `CLIENT_CA` | empty | mTLS material |
