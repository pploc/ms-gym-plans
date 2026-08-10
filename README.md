# ms-gym-plans

Plans catalog service for gym locations and membership plans.

## Scope (G7 / V1)

- Owns `plans_db` tables `gym_locations` and `membership_plans`
- Public **Spring HTTP** on `8080` for catalog CRUD/list (Kong target)
- Native gRPC on `50051` for the same public methods plus internal workload RPCs:
  - `GetActiveGym` — Identifier only
  - `ResolvePurchasablePlan` — Member only
- No Kafka, outbox, cache, scheduler, Redis, Payment client
- No separate Go gateway process — Java service serves HTTP and gRPC

## Dependencies

- Java 26, Spring Boot 4.1
- `com.gym:common-java:2.0.2` (includes `ProtobufJsonHttpMessageConverter` auto-config)
- `com.gym.proto:gym-proto-java:5.0.0`
- PostgreSQL (`plans_db`)

Public HTTP request/response bodies are generated protobuf messages (`com.gym.proto.plans.v1.*`) with camelCase JSON field names (`chainId`, `priceVnd`). Path segments stay contract routes (`/gyms/{gym_id}/plans`). Do not use `mavenLocal()` or SNAPSHOT common-java for release verification.

## Run locally

```bash
export GITHUB_TOKEN=...
export GITHUB_ACTOR=pploc

./gradlew startEnv
./gradlew bootRun
```

`bootRun` auto-runs `ensureLocalCerts` → `certs/local/` when missing. Defaults:

- gRPC mTLS on `:50051` (`certs/local/server.*` + `ca.crt`)
- HTTP catalog on `:8080` (claim headers, no JWT)

Stop deps:

```bash
./gradlew stopEnv
```

- HTTP: `http://localhost:8080`
- gRPC mTLS: `localhost:50051` with `certs/local/client-postman.*`
- Bodies / Postman: [LOCAL_TESTING.md](./LOCAL_TESTING.md)

Plaintext override (skip mTLS):

```bash
export PLANS_GRPC_TLS_ENABLED=false
export PLANS_GRPC_ALLOW_PLAINTEXT=true
./gradlew bootRun
```

## Public HTTP routes

| Method | Path | Roles |
|--------|------|--------|
| `POST` | `/api/v1/gyms` | `SUPER_ADMIN` |
| `PUT` | `/api/v1/gyms/{id}` | `SUPER_ADMIN` |
| `GET` | `/api/v1/gyms` | authenticated |
| `GET` | `/api/v1/gyms/{id}` | authenticated |
| `POST` | `/api/v1/gyms/{gym_id}/plans` | `SUPER_ADMIN` |
| `PUT` | `/api/v1/plans/{id}` | `SUPER_ADMIN` |
| `GET` | `/api/v1/gyms/{gym_id}/plans` | authenticated |
| `GET` | `/api/v1/plans/{id}` | authenticated |

Kong injects only trusted `x-user-id` and `x-user-role` headers. Gym context comes from request paths and fields.

Internal RPCs have **no** HTTP mapping.

## Local gRPC / Postman

See [LOCAL_TESTING.md](./LOCAL_TESTING.md) for claim headers and full request bodies (gRPC + HTTP).

## Tests and CI

```bash
./gradlew clean build
```

GitHub Actions (`.github/workflows/ci.yml`) reuses `gym-infra`:

- `java-ci.yml@develop` with `gradle_args: build`
- `docker-build.yml@develop` for `ms-gym-plans` image push on develop/main/feature/hotfix

## Config

| Env | Default | Purpose |
|-----|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/plans_db` | DB |
| `SERVER_PORT` | `8080` | Public HTTP |
| `GRPC_SERVER_PORT` | `50051` | gRPC |
| `PLANS_GRPC_TLS_ENABLED` | `true` | mTLS |
| `PLANS_GRPC_ALLOW_PLAINTEXT` | `false` | test-only plaintext |
| `PLANS_GRPC_SERVER_CERT` | `certs/local/server.crt` | server chain; **prod must override** (secret mount) |
| `PLANS_GRPC_SERVER_KEY` | `certs/local/server.key` | server key; **prod must override** |
| `PLANS_GRPC_CLIENT_CA` | `certs/local/ca.crt` | client trust CA; **prod must override** |

Local defaults point at `certs/local/` (gitignored; `bootRun` → `ensureLocalCerts`). Never bake those files into the image. Production always sets the three `PLANS_GRPC_*` path env vars to real certs.
