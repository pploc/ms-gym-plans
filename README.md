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
- `com.gym:common-java:2.0.1` (includes `ProtobufJsonHttpMessageConverter` auto-config)
- `com.gym.proto:gym-proto-java:3.0.0`
- PostgreSQL (`plans_db`)

Public HTTP request/response bodies are generated protobuf messages (`com.gym.proto.plans.v1.*`) with snake_case JSON field names. Do not use `mavenLocal()` or SNAPSHOT common-java for release verification.

## Run locally

```bash
export GITHUB_TOKEN=...
export GITHUB_ACTOR=pploc

./gradlew startEnv
./gradlew bootRun
```

Stop deps:

```bash
./gradlew stopEnv
```

- HTTP (public catalog): `http://localhost:8080`
- gRPC (public + internal): `localhost:50051`

## Public HTTP routes

| Method | Path | Roles |
|--------|------|--------|
| `POST` | `/api/v1/gyms` | `SUPER_ADMIN` |
| `PUT` | `/api/v1/gyms/{id}` | `ADMIN`, `SUPER_ADMIN` |
| `GET` | `/api/v1/gyms` | authenticated |
| `GET` | `/api/v1/gyms/{id}` | authenticated |
| `POST` | `/api/v1/gyms/{gym_id}/plans` | `ADMIN`, `SUPER_ADMIN` |
| `PUT` | `/api/v1/plans/{id}` | `ADMIN`, `SUPER_ADMIN` |
| `GET` | `/api/v1/gyms/{gym_id}/plans` | authenticated |
| `GET` | `/api/v1/plans/{id}` | authenticated |

Kong injects trusted headers: `x-user-id`, `x-user-role`, `x-gym-id`, `x-membership-status`.

Internal RPCs have **no** HTTP mapping.

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
| `PLANS_GRPC_SERVER_CERT` / `KEY` / `CLIENT_CA` | empty | mTLS material |
