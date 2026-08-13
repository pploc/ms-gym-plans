# Plans — local testing (gRPC + HTTP)

Examples for current Stage 0 Plans service with `gym-proto` `5.0.0`.

## Start

```bash
cd ms-gym-plans
./gradlew startEnv
./gradlew bootRun
```

- HTTP: `http://localhost:8080`
- gRPC mTLS: `localhost:50051`
- Stop dependencies: `./gradlew stopEnv`

```bash
PROTO_DIR=../gym-proto/proto
C=certs/local
MTLS_PUBLIC=(-cacert "$C/ca.crt" -cert "$C/client-gateway.crt" -key "$C/client-gateway.key")
H_SUPER=(-H 'x-user-id: super-1' -H 'x-user-role: SUPER_ADMIN')
H_CUSTOMER=(-H 'x-user-id: customer-1' -H 'x-user-role: CUSTOMER')
```

Public claim-bearing RPCs require generated gateway client certificate plus `x-user-id` and `x-user-role` forwarded after Kong JWT verification. Other CA-valid client certificates cannot supply trusted claims. Gym context comes from request paths and fields. `ADMIN` has no mutation authority.

## Policy

| RPC / HTTP operation | Authorization |
|---|---|
| Create or update gym | `SUPER_ADMIN` only |
| Create or update plan | `SUPER_ADMIN` only |
| Get or list gyms/plans | authenticated role |
| `GetActiveGym` | Identifier certificate only |
| `ResolvePurchasablePlan` | Member certificate only |

## Public gRPC examples

### CreateGymLocation

```bash
grpcurl "${MTLS_PUBLIC[@]}" -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H_SUPER[@]}" \
  -d '{"chainId":"chain-vn-hcm","name":"Saigon Landmark 81","address":"720A Dien Bien Phu","city":"Ho Chi Minh"}' \
  localhost:50051 plans.v1.PlansService/CreateGymLocation
```

### CreateMembershipPlan

```bash
grpcurl "${MTLS_PUBLIC[@]}" -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H_SUPER[@]}" \
  -d '{
    "gymId":"GYM_ID",
    "name":"Premium Monthly Unlimited",
    "planType":"PLAN_TYPE_MONTHLY",
    "durationDays":30,
    "priceVnd":899000,
    "description":"Full access",
    "active":true
  }' \
  localhost:50051 plans.v1.PlansService/CreateMembershipPlan
```

### ListMembershipPlans

```bash
grpcurl "${MTLS_PUBLIC[@]}" -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H_CUSTOMER[@]}" \
  -d '{"gymId":"GYM_ID","planType":"PLAN_TYPE_MONTHLY","active":true}' \
  localhost:50051 plans.v1.PlansService/ListMembershipPlans
```

## Internal workload examples

### GetActiveGym — Identifier only

```bash
grpcurl -cacert "$C/ca.crt" -cert "$C/client-identifier.crt" -key "$C/client-identifier.key" \
  -import-path "$PROTO_DIR" -proto plans/v1/plans.proto \
  -d '{"gymId":"GYM_ID"}' \
  localhost:50051 plans.v1.PlansService/GetActiveGym
```

### ResolvePurchasablePlan — Member only

```bash
grpcurl -cacert "$C/ca.crt" -cert "$C/client-member.crt" -key "$C/client-member.key" \
  -import-path "$PROTO_DIR" -proto plans/v1/plans.proto \
  -d '{"planId":"PLAN_ID","gymId":"GYM_ID"}' \
  localhost:50051 plans.v1.PlansService/ResolvePurchasablePlan
```

Resolution fails closed when gym is missing or inactive, plan is missing or inactive, or plan does not belong to requested gym. Response supplies canonical plan type, duration, and VND price.

Spring MVC remains active on `:8080` during Stage 2. Its removal and Kong HTTPS/JSON route verification are Stage 4 and Stage 3 work.

## HTTP routes

| Method | Path | Authorization |
|---|---|---|
| `POST` | `/api/v1/gyms` | `SUPER_ADMIN` |
| `PUT` | `/api/v1/gyms/{id}` | `SUPER_ADMIN` |
| `GET` | `/api/v1/gyms` | authenticated |
| `GET` | `/api/v1/gyms/{id}` | authenticated |
| `POST` | `/api/v1/gyms/{gym_id}/plans` | `SUPER_ADMIN` |
| `PUT` | `/api/v1/plans/{id}` | `SUPER_ADMIN` |
| `GET` | `/api/v1/gyms/{gym_id}/plans` | authenticated |
| `GET` | `/api/v1/plans/{id}` | authenticated |

Example:

```bash
curl -fsS -X POST http://localhost:8080/api/v1/gyms \
  -H 'Content-Type: application/json' \
  -H 'x-user-id: super-1' \
  -H 'x-user-role: SUPER_ADMIN' \
  -d '{"chainId":"chain-vn-hcm","name":"Saigon Landmark 81","address":"720A Dien Bien Phu","city":"Ho Chi Minh"}'
```

## Negative checks

- `ADMIN` gym or plan mutation is denied on gRPC and HTTP.
- Forged `x-gym-id` or `x-membership-status` grants no authority.
- Identifier certificate cannot call `ResolvePurchasablePlan`.
- Member certificate cannot call `GetActiveGym`.
- Gateway certificate cannot call workload RPCs.
- Identifier, Member, Check-in, Notification, and Postman certificates cannot use forged `x-user-*` metadata on public gRPC.
