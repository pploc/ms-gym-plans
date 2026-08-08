# Plans — local testing (gRPC + HTTP)

Copy-paste bodies for Postman / grpcurl. No Identifier or Kong required: inject trusted claims headers the same way Kong would.

## 1. Start (default = mTLS)

```bash
cd ms-gym-plans
./gradlew startEnv
./gradlew bootRun
```

`bootRun` runs `ensureLocalCerts` if `certs/local/` incomplete. Defaults in `application.yml`:

| Env / property | Default |
|----------------|---------|
| `PLANS_GRPC_TLS_ENABLED` | `true` |
| `PLANS_GRPC_SERVER_CERT` | `certs/local/server.crt` |
| `PLANS_GRPC_SERVER_KEY` | `certs/local/server.key` |
| `PLANS_GRPC_CLIENT_CA` | `certs/local/ca.crt` |

| Port | Protocol |
|------|----------|
| `8080` | Public HTTP (camelCase JSON; no gRPC mTLS) |
| `50051` | gRPC **mTLS** (client cert required) |
| `5433` | Postgres `plans_db` |

Stop deps: `./gradlew stopEnv`.

Plaintext override only if needed:

```bash
export PLANS_GRPC_TLS_ENABLED=false
export PLANS_GRPC_ALLOW_PLAINTEXT=true
./gradlew bootRun
```

Proto: `../gym-proto/proto/plans/v1/plans.proto` (import path root `../gym-proto/proto`).

### Postman / grpcurl mTLS

1. Settings → Certificates → host `localhost` port `50051`  
   - CRT/KEY: `certs/local/client-postman.crt` + `.key`  
   - or PFX: `client-postman.p12` password `changeit`  
   - trust `ca.crt`
2. Metadata for **public** RPCs: `x-user-id`, `x-user-role`, `x-membership-status` (omit blank `x-gym-id`).
3. Internal:
   - `GetActiveGym` → `client-identifier.*` (SAN `ms-gym-identifier`)
   - `ResolvePurchasablePlan` → `client-member.*` (SAN `ms-gym-member`)

Client cert = peer identity. Role still from claim metadata (local fake Kong).

## 2. Auth — no JWT / no Identifier

**Prod path:** FE → Kong validates Identifier JWT → Kong **strips** client `x-user-*` → injects trusted claims → Plans HTTP.

**Local path:** skip Identifier + Kong. You inject the same claim headers yourself. Plans never sees an access token; it only reads:

Plans does **not** accept JWT. Send Kong-style metadata / headers:

| Key | Example | Notes |
|-----|---------|--------|
| `x-user-id` | `super-1` | required |
| `x-user-role` | `SUPER_ADMIN` | `CUSTOMER` \| `TRAINER` \| `ADMIN` \| `SUPER_ADMIN` |
| `x-membership-status` | `NONE` | `NONE` \| `ACTIVE` \| `PAUSED` \| `EXPIRED` |
| `x-gym-id` | `<gym-uuid>` | required for scoped `ADMIN` writes; **omit** when empty (do not send blank) |

### Role cheat sheet

| RPC / HTTP | Roles |
|------------|--------|
| Create gym | `SUPER_ADMIN` |
| Update gym, create/update plan | `ADMIN` or `SUPER_ADMIN` (`ADMIN` only if `x-gym-id` matches that gym) |
| Get / list gyms & plans | any authenticated role above |
| `GetActiveGym` | mTLS workload **Identifier** only — not for Postman plaintext |
| `ResolvePurchasablePlan` | mTLS workload **Member** only — not for Postman plaintext |

## 3. Postman — gRPC

1. New → **gRPC** request.
2. Server URL: `grpc://localhost:50051` (or `localhost:50051` + enable plaintext / insecure if prompted).
3. Service definition: import `plans/v1/plans.proto` with import path = `gym-proto/proto`.
4. Method: pick `plans.v1.PlansService/...`.
5. **Metadata** tab: add the four keys above (omit `x-gym-id` for super-admin create gym).
6. **Message** tab: paste JSON body (protobuf JSON = **camelCase**).

Reflection alternative (if enabled on server):

```bash
grpcurl -plaintext localhost:50051 list
grpcurl -plaintext localhost:50051 describe plans.v1.PlansService
```

## 4. gRPC bodies (camelCase)

Replace UUIDs after create responses.

### CreateGymLocation — `SUPER_ADMIN`

```json
{
  "chainId": "chain-vn-hcm",
  "name": "Saigon Landmark 81",
  "address": "720A Dien Bien Phu, Binh Thanh",
  "city": "Ho Chi Minh"
}
```

### UpdateGymLocation — full replace

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

### GetGymLocation

```json
{
  "id": "GYM_ID"
}
```

### ListGymLocations

```json
{
  "chainId": "chain-vn-hcm",
  "city": "Ho Chi Minh",
  "status": "ACTIVE"
}
```

Empty strings omit filters:

```json
{}
```

### CreateMembershipPlan — path gym is request field for gRPC

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

`active` optional on create (default `true`).

### UpdateMembershipPlan — full replace (`gymId` not updatable)

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

### GetMembershipPlan

```json
{
  "id": "PLAN_ID"
}
```

### ListMembershipPlans

```json
{
  "gymId": "GYM_ID",
  "planType": "MONTHLY",
  "active": true
}
```

### Internal (skip in Postman plaintext)

```json
{ "gymId": "GYM_ID" }
```

```json
{ "planId": "PLAN_ID", "gymId": "GYM_ID" }
```

These need real mTLS client certs (`GetActiveGym` / `ResolvePurchasablePlan`).

## 5. grpcurl one-liners (mTLS default)

```bash
# from ms-gym-plans
PROTO_DIR=../gym-proto/proto
C=certs/local
H=(-H 'x-user-id: super-1' -H 'x-user-role: SUPER_ADMIN' -H 'x-membership-status: NONE')

grpcurl -cacert "$C/ca.crt" -cert "$C/client-postman.crt" -key "$C/client-postman.key" \
  -import-path "$PROTO_DIR" -proto plans/v1/plans.proto "${H[@]}" \
  -d '{"chainId":"chain-vn-hcm","name":"Saigon Landmark 81","address":"720A Dien Bien Phu","city":"Ho Chi Minh"}' \
  localhost:50051 plans.v1.PlansService/CreateGymLocation

# internal GetActiveGym (Identifier SAN)
grpcurl -cacert "$C/ca.crt" -cert "$C/client-identifier.crt" -key "$C/client-identifier.key" \
  -import-path "$PROTO_DIR" -proto plans/v1/plans.proto \
  -d '{"gymId":"GYM_ID"}' \
  localhost:50051 plans.v1.PlansService/GetActiveGym
```

If field ignored, try snake_case (`chain_id`); parser accepts both.

## 6. Postman — HTTP (REST)

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

Headers: `Content-Type: application/json`, `x-user-id`, `x-user-role: SUPER_ADMIN`, `x-membership-status: NONE`

```json
{
  "chainId": "chain-vn-hcm",
  "name": "Saigon Landmark 81",
  "address": "720A Dien Bien Phu, Binh Thanh",
  "city": "Ho Chi Minh"
}
```

### POST `/api/v1/gyms/{gym_id}/plans`

Also set `x-gym-id: {gym_id}` when roleing as `ADMIN`.

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

(`gymId` comes from the path, not the body.)

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

## 7. Suggested happy path

1. Create gym as `SUPER_ADMIN` → copy `id`.
2. Create monthly plan under that gym → copy plan `id`.
3. List plans as `CUSTOMER`.
4. Update plan `active: false`, list with `active=true` → empty/hidden.
5. Update gym `status: CLOSED`.

## 8. Common failures

| Symptom | Cause |
|---------|--------|
| `UNAUTHENTICATED` / 401 | missing `x-user-id` or `x-user-role`, or blank `x-gym-id` header |
| `PERMISSION_DENIED` / 403 | wrong role or `ADMIN` gym mismatch |
| `INVALID_ARGUMENT` / 400 | bad `planType`, duration rules, empty required strings |
| connection TLS error | forgot `PLANS_GRPC_TLS_ENABLED=false` + allow plaintext |
