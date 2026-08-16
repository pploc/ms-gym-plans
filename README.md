# ms-gym-plans

Plans catalog service for gym locations and membership plans.

## Scope (G7 / V1)

- Owns `plans_db` tables `gym_locations` and `membership_plans`
- Native gRPC on `50051`:
  - catalog CRUD/list accepts end-user claims only from verified generated gateway mTLS identity
  - `GetActiveGym` — Identifier only
  - `ResolvePurchasablePlan` — Member only
  - `ValidateCheckInGym` — Check-in only
- No Kafka, outbox, cache, scheduler, Redis, Payment client
- No separate Go gateway process — Java service serves HTTP and gRPC

## Dependencies

- Java 26, Spring Boot 4.1
- `com.gym:common-java:3.0.0-rc.1` (includes `ProtobufJsonHttpMessageConverter` auto-config)
- `com.gym.proto:gym-proto-java:7.0.2`
- PostgreSQL (`plans_db`)

Browser JSON reaches Plans only through Kong and generated Go grpc-gateway. Plans keeps `8080` for Actuator, probes, and Prometheus; business paths under `/api/v1/**` return `404`. Do not use `mavenLocal()` or SNAPSHOT common-java for release verification.

## Run locally

```bash
export GITHUB_TOKEN=...
export GITHUB_ACTOR=pploc

./gradlew startEnv
./gradlew bootRun
```

`bootRun` auto-runs `ensureLocalCerts` → `certs/local/` when missing. Defaults:

- gRPC mTLS on `:50051` (`certs/local/server.*` + `ca.crt`)
- public native gRPC requires `certs/local/client-gateway.*` plus identity/role metadata
- Actuator/probes/metrics on `:8080`; direct business HTTP is disabled

Stop deps:

```bash
./gradlew stopEnv
```

- Actuator: `http://localhost:8080/actuator/health`
- gRPC mTLS: `localhost:50051` with `certs/local/client-gateway.*` for public RPCs; exact workload certificates for internal RPCs
- Browser API: Kong HTTPS, then generated gateway; gRPC bodies: [LOCAL_TESTING.md](./LOCAL_TESTING.md)

Plaintext override (skip mTLS):

```bash
export PLANS_GRPC_TLS_ENABLED=false
export PLANS_GRPC_ALLOW_PLAINTEXT=true
./gradlew bootRun
```

## HTTP surface

`8080` exposes Actuator health and Prometheus only. Direct business paths, including `/api/v1/**`, return `404`.

Kong injects only trusted `x-user-id` and `x-user-role` metadata, and generated gateway forwards them over mTLS. Plans accepts claim-bearing gRPC only from generated gateway DNS/SPIFFE client SAN; Kong, Identifier, Member, Check-in, Notification, and Postman identities cannot forge those claims. Gym context comes from request paths and fields.

Internal RPCs have no HTTP mapping. Kong exposes only generated-gateway public routes; no reflection or workload routes.

## Local gRPC / Postman

See [LOCAL_TESTING.md](./LOCAL_TESTING.md) for claim headers and full request bodies (gRPC + HTTP).

## Tests and CI

```bash
./gradlew clean build
```

GitHub Actions (`.github/workflows/ci.yml`) pins reusable `gym-infra` workflow revisions:

- `java-ci.yml@4aa5f289f526b3fc2bb22790fe6e0dc2b49fe243` with `gradle_args: build`
- `docker-build.yml@ace7a285cc14b3e1121cac17bcfa9ea0ce5d1102` publishes only on a `develop` `workflow_dispatch` with `publish_image=true`; image tag is source SHA only

## Config

| Env | Default | Purpose |
|-----|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/plans_db` | DB |
| `SERVER_PORT` | `8080` | Actuator, probes, and metrics |
| `GRPC_SERVER_PORT` | `50051` | gRPC |
| `PLANS_GRPC_TLS_ENABLED` | `true` | mTLS |
| `PLANS_GRPC_ALLOW_PLAINTEXT` | `false` | test-only plaintext |
| `PLANS_GRPC_SERVER_CERT` | `certs/local/server.crt` | server chain; **prod must override** (secret mount) |
| `PLANS_GRPC_SERVER_KEY` | `certs/local/server.key` | server key; **prod must override** |
| `PLANS_GRPC_CLIENT_CA` | `certs/local/ca.crt` | client trust CA; **prod must override** |

Local defaults point at `certs/local/` (gitignored; `bootRun` → `ensureLocalCerts`). Never bake those files into the image. Production always sets the three `PLANS_GRPC_*` path env vars to real certs.
