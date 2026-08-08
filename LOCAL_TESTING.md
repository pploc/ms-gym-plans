# Plans — local testing (gRPC + HTTP)

Copy-paste bodies for Postman / grpcurl. No Identifier or Kong required: inject trusted claim metadata the same way Kong would.

## 1. Start (default = mTLS)

```bash
cd ms-gym-plans
./gradlew startEnv
./gradlew bootRun
```

`bootRun` runs `ensureLocalCerts` if `certs/local/` incomplete.

| Port | Protocol |
|------|----------|
| `8080` | Public HTTP (camelCase JSON; no gRPC mTLS) |
| `50051` | gRPC **mTLS** (client cert required) |
| `5433` | Postgres `plans_db` |

Stop deps: `./gradlew stopEnv`.

Plaintext override:

```bash
export PLANS_GRPC_TLS_ENABLED=false
export PLANS_GRPC_ALLOW_PLAINTEXT=true
./gradlew bootRun
```

Proto: `../gym-proto/proto/plans/v1/plans.proto` (import path root `../gym-proto/proto`).

### Shared shell vars (grpcurl)

```bash
# from ms-gym-plans
PROTO_DIR=../gym-proto/proto
C=certs/local
MTLS=(-cacert "$C/ca.crt" -cert "$C/client-postman.crt" -key "$C/client-postman.key")
H_SUPER=(-H 'x-user-id: super-1' -H 'x-user-role: SUPER_ADMIN' -H 'x-membership-status: NONE')
H_ADMIN=(-H 'x-user-id: admin-1' -H 'x-user-role: ADMIN' -H 'x-membership-status: NONE' -H 'x-gym-id: GYM_ID')
H_CUST=(-H 'x-user-id: cust-1' -H 'x-user-role: CUSTOMER' -H 'x-membership-status: NONE')
```

### Postman gRPC setup (once)

1. Certificates → host `localhost` port `50051` → `client-postman.crt` + `.key` (or `.p12` / `changeit`); trust `ca.crt`.
2. New gRPC → URL `localhost:50051` (TLS on).
3. Import `plans/v1/plans.proto` (import path = `gym-proto/proto`).
4. Method: `plans.v1.PlansService/<Method>`.
5. **Metadata** tab = claim keys (not REST Headers).
6. **Message** tab = JSON below (camelCase).

Internal RPCs: switch client cert to `client-identifier.*` or `client-member.*`.

---

## 2. Auth metadata

| Key | Example | Notes |
|-----|---------|--------|
| `x-user-id` | `super-1` | required for public RPCs |
| `x-user-role` | `SUPER_ADMIN` | `CUSTOMER` \| `TRAINER` \| `ADMIN` \| `SUPER_ADMIN` |
| `x-membership-status` | `NONE` | `NONE` \| `ACTIVE` \| `PAUSED` \| `EXPIRED` |
| `x-gym-id` | `<gym-uuid>` | required for scoped `ADMIN` writes; **omit** if empty |

| RPC | Roles / cert |
|-----|----------------|
| CreateGymLocation | `SUPER_ADMIN` + postman cert |
| UpdateGymLocation, Create/Update plan | `ADMIN` / `SUPER_ADMIN` + postman cert |
| Get / list gyms & plans | any authenticated role + postman cert |
| GetActiveGym | **identifier** client cert only (no user metadata) |
| ResolvePurchasablePlan | **member** client cert only (no user metadata) |

---

## 3. Public RPCs — body + grpcurl

Replace `GYM_ID` / `PLAN_ID` after creates.

### CreateGymLocation

- **Service:** `plans.v1.PlansService/CreateGymLocation`
- **Metadata:** `x-user-id=super-1`, `x-user-role=SUPER_ADMIN`, `x-membership-status=NONE`
- **Postman Message (copy):**

```json
{
  "chainId": "chain-vn-hcm",
  "name": "Saigon Landmark 81",
  "address": "720A Dien Bien Phu, Binh Thanh",
  "city": "Ho Chi Minh"
}
```

```bash
grpcurl "${MTLS[@]}" -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H_SUPER[@]}" \
  -d '{
  "chainId": "chain-vn-hcm",
  "name": "Saigon Landmark 81",
  "address": "720A Dien Bien Phu, Binh Thanh",
  "city": "Ho Chi Minh"
}' \
  localhost:50051 plans.v1.PlansService/CreateGymLocation
```

---

### UpdateGymLocation

- **Service:** `plans.v1.PlansService/UpdateGymLocation`
- **Metadata:** `SUPER_ADMIN` or `ADMIN` + `x-gym-id=GYM_ID`
- **Postman Message:**

```json
{
  "id": "GYM_ID",
  "chainId": "chain-vn-hcm",
  "name": "Saigon Landmark 81 - Renamed",
  "address": "720A Dien Bien Phu, Binh Thanh, HCMC",
  "city": "Ho Chi Minh",
  "status": "ACTIVE"
}
```

`status`: `ACTIVE` | `CLOSED`.

```bash
grpcurl "${MTLS[@]}" -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H_SUPER[@]}" \
  -d '{
  "id": "GYM_ID",
  "chainId": "chain-vn-hcm",
  "name": "Saigon Landmark 81 - Renamed",
  "address": "720A Dien Bien Phu, Binh Thanh, HCMC",
  "city": "Ho Chi Minh",
  "status": "ACTIVE"
}' \
  localhost:50051 plans.v1.PlansService/UpdateGymLocation
```

---

### GetGymLocation

- **Service:** `plans.v1.PlansService/GetGymLocation`
- **Metadata:** any authenticated role
- **Postman Message:**

```json
{
  "id": "GYM_ID"
}
```

```bash
grpcurl "${MTLS[@]}" -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H_CUST[@]}" \
  -d '{
  "id": "GYM_ID"
}' \
  localhost:50051 plans.v1.PlansService/GetGymLocation
```

---

### ListGymLocations

- **Service:** `plans.v1.PlansService/ListGymLocations`
- **Metadata:** any authenticated role
- **Postman Message (filtered):**

```json
{
  "chainId": "chain-vn-hcm",
  "city": "Ho Chi Minh",
  "status": "ACTIVE"
}
```

**Postman Message (no filters):**

```json
{}
```

```bash
grpcurl "${MTLS[@]}" -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H_CUST[@]}" \
  -d '{
  "chainId": "chain-vn-hcm",
  "city": "Ho Chi Minh",
  "status": "ACTIVE"
}' \
  localhost:50051 plans.v1.PlansService/ListGymLocations
```

---

### CreateMembershipPlan

- **Service:** `plans.v1.PlansService/CreateMembershipPlan`
- **Metadata:** `SUPER_ADMIN` or `ADMIN` + matching `x-gym-id`
- **Postman Message:**

```json
{
  "gymId": "GYM_ID",
  "name": "Premium Monthly Unlimited",
  "planType": "MONTHLY",
  "durationDays": 30,
  "priceVnd": 899000,
  "description": "Full access all zones, locker, towel, group classes",
  "active": true
}
```

| `planType` | `durationDays` |
|------------|----------------|
| `MONTHLY` / `YEARLY` | required, positive |
| `LIFETIME` | omit / unset |

```bash
grpcurl "${MTLS[@]}" -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H_SUPER[@]}" \
  -d '{
  "gymId": "GYM_ID",
  "name": "Premium Monthly Unlimited",
  "planType": "MONTHLY",
  "durationDays": 30,
  "priceVnd": 899000,
  "description": "Full access all zones, locker, towel, group classes",
  "active": true
}' \
  localhost:50051 plans.v1.PlansService/CreateMembershipPlan
```

---

### UpdateMembershipPlan

- **Service:** `plans.v1.PlansService/UpdateMembershipPlan`
- **Metadata:** `SUPER_ADMIN` or `ADMIN` + gym scope
- **Postman Message:**

```json
{
  "id": "PLAN_ID",
  "name": "Premium Monthly Unlimited Plus",
  "planType": "MONTHLY",
  "durationDays": 31,
  "priceVnd": 999000,
  "description": "Full access + guest pass",
  "active": true
}
```

(`gymId` not updatable.)

```bash
grpcurl "${MTLS[@]}" -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H_SUPER[@]}" \
  -d '{
  "id": "PLAN_ID",
  "name": "Premium Monthly Unlimited Plus",
  "planType": "MONTHLY",
  "durationDays": 31,
  "priceVnd": 999000,
  "description": "Full access + guest pass",
  "active": true
}' \
  localhost:50051 plans.v1.PlansService/UpdateMembershipPlan
```

---

### GetMembershipPlan

- **Service:** `plans.v1.PlansService/GetMembershipPlan`
- **Metadata:** any authenticated role
- **Postman Message:**

```json
{
  "id": "PLAN_ID"
}
```

```bash
grpcurl "${MTLS[@]}" -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H_CUST[@]}" \
  -d '{
  "id": "PLAN_ID"
}' \
  localhost:50051 plans.v1.PlansService/GetMembershipPlan
```

---

### ListMembershipPlans

- **Service:** `plans.v1.PlansService/ListMembershipPlans`
- **Metadata:** any authenticated role
- **Postman Message:**

```json
{
  "gymId": "GYM_ID",
  "planType": "MONTHLY",
  "active": true
}
```

```bash
grpcurl "${MTLS[@]}" -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H_CUST[@]}" \
  -d '{
  "gymId": "GYM_ID",
  "planType": "MONTHLY",
  "active": true
}' \
  localhost:50051 plans.v1.PlansService/ListMembershipPlans
```

---

## 4. Internal RPCs — body + grpcurl

No `x-user-*` as workload proof. Use workload client cert.

### GetActiveGym

- **Service:** `plans.v1.PlansService/GetActiveGym`
- **Client cert:** `client-identifier.crt` / `.key` (SAN `ms-gym-identifier`)
- **Metadata:** none required
- **Postman Message:**

```json
{
  "gymId": "GYM_ID"
}
```

```bash
grpcurl -cacert "$C/ca.crt" -cert "$C/client-identifier.crt" -key "$C/client-identifier.key" \
  -import-path "$PROTO_DIR" -proto plans/v1/plans.proto \
  -d '{
  "gymId": "GYM_ID"
}' \
  localhost:50051 plans.v1.PlansService/GetActiveGym
```

---

### ResolvePurchasablePlan

- **Service:** `plans.v1.PlansService/ResolvePurchasablePlan`
- **Client cert:** `client-member.crt` / `.key` (SAN `ms-gym-member`)
- **Metadata:** none required
- **Postman Message:**

```json
{
  "planId": "PLAN_ID",
  "gymId": "GYM_ID"
}
```

```bash
grpcurl -cacert "$C/ca.crt" -cert "$C/client-member.crt" -key "$C/client-member.key" \
  -import-path "$PROTO_DIR" -proto plans/v1/plans.proto \
  -d '{
  "planId": "PLAN_ID",
  "gymId": "GYM_ID"
}' \
  localhost:50051 plans.v1.PlansService/ResolvePurchasablePlan
```

---

## 5. Postman — HTTP (REST)

Base: `http://localhost:8080`  
Same claim **headers** (not metadata). Bodies **camelCase**.

| Method | Path | Role |
|--------|------|------|
| `POST` | `/api/v1/gyms` | `SUPER_ADMIN` |
| `PUT` | `/api/v1/gyms/{id}` | `ADMIN` / `SUPER_ADMIN` |
| `GET` | `/api/v1/gyms/{id}` | authenticated |
| `GET` | `/api/v1/gyms?chainId=&city=&status=` | authenticated |
| `POST` | `/api/v1/gyms/{gym_id}/plans` | `ADMIN` / `SUPER_ADMIN` |
| `PUT` | `/api/v1/plans/{id}` | `ADMIN` / `SUPER_ADMIN` |
| `GET` | `/api/v1/plans/{id}` | authenticated |
| `GET` | `/api/v1/gyms/{gym_id}/plans?planType=&active=` | authenticated |

### POST `/api/v1/gyms`

```json
{
  "chainId": "chain-vn-hcm",
  "name": "Saigon Landmark 81",
  "address": "720A Dien Bien Phu, Binh Thanh",
  "city": "Ho Chi Minh"
}
```

### POST `/api/v1/gyms/{gym_id}/plans`

(`gymId` from path only.)

```json
{
  "name": "Yearly Gold",
  "planType": "YEARLY",
  "durationDays": 365,
  "priceVnd": 8999000,
  "description": "12-month gold membership",
  "active": true
}
```

### PUT `/api/v1/gyms/{id}`

```json
{
  "chainId": "chain-vn-hcm",
  "name": "Saigon Landmark 81 - Renamed",
  "address": "720A Dien Bien Phu, Binh Thanh, HCMC",
  "city": "Ho Chi Minh",
  "status": "CLOSED"
}
```

### PUT `/api/v1/plans/{id}`

```json
{
  "name": "Yearly Gold Plus",
  "planType": "YEARLY",
  "durationDays": 365,
  "priceVnd": 9999000,
  "description": "updated",
  "active": false
}
```

No delete APIs — use `status: CLOSED` / `active: false`.

---

## 6. Happy path

1. CreateGymLocation → copy `id` → `GYM_ID`
2. CreateMembershipPlan → copy `id` → `PLAN_ID`
3. ListMembershipPlans as `CUSTOMER`
4. UpdateMembershipPlan `active: false`
5. GetActiveGym / ResolvePurchasablePlan with workload certs

---

## 7. Common failures

| Symptom | Cause |
|---------|--------|
| `UNAUTHENTICATED` / 401 | missing `x-user-id` or `x-user-role`, or blank `x-gym-id` |
| `PERMISSION_DENIED` / 403 | wrong role, gym mismatch, or wrong client cert on internal RPC |
| `INVALID_ARGUMENT` / 400 | bad `planType`, duration rules, empty required strings |
| TLS / dial fail | missing client cert or wrong CA |
| field ignored | try snake_case (`chain_id`); parser accepts both |
