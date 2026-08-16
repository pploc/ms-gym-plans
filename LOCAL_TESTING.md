# Plans — local testing (gRPC)

Examples for Plans with `gym-proto` `7.0.2`.

## Start

```bash
cd ms-gym-plans
./gradlew startEnv
./gradlew bootRun
```

- Actuator: `http://localhost:8080/actuator/health`
- gRPC mTLS: `localhost:50051`
- Browser JSON: Kong HTTPS, then generated Go grpc-gateway
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

| RPC | Authorization |
|---|---|
| Create or update gym | `SUPER_ADMIN` only |
| Create or update plan | `SUPER_ADMIN` only |
| Get or list gyms/plans | authenticated role |
| `GetActiveGym` | Identifier certificate only |
| `ResolvePurchasablePlan` | Member certificate only |
| `ValidateCheckInGym` | Check-in certificate only |

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

### ValidateCheckInGym — Check-in only

```bash
grpcurl -cacert "$C/ca.crt" -cert "$C/client-checkin.crt" -key "$C/client-checkin.key" \
  -import-path "$PROTO_DIR" -proto plans/v1/plans.proto \
  -d '{"gymId":"GYM_ID"}' \
  localhost:50051 plans.v1.PlansService/ValidateCheckInGym
```

Resolution fails closed when gym is missing or inactive, plan is missing or inactive, or plan does not belong to requested gym. Response supplies canonical plan type, duration, and VND price.

## HTTP isolation

`8080` serves Actuator, probes, and Prometheus only. Direct business HTTP is disabled:

```bash
curl -i http://localhost:8080/api/v1/gyms
# HTTP/1.1 404
curl -fsS http://localhost:8080/actuator/health/liveness
curl -fsS http://localhost:8080/actuator/health/readiness
```

Kong HTTPS is required for browser JSON. It authenticates JWTs and forwards trusted claims through generated gateway mTLS.

## Negative checks

- `ADMIN` gym or plan mutation is denied on gRPC.
- Direct `/api/v1/**` requests on `:8080` return `404`.
- Identifier certificate cannot call `ResolvePurchasablePlan`.
- Member certificate cannot call `GetActiveGym`.
- Gateway certificate cannot call workload RPCs.
- Identifier, Member, Check-in, Notification, and Postman certificates cannot use forged `x-user-*` metadata on public gRPC.
