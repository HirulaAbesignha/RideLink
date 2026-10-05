# RideLink Postman Testing Guide

This guide matches the implementation in `D:\RideLink` at commit `ec30462` on branch `fix/submission-readiness`.

## 1. Verified local result

The full live workflow was tested on 5 October 2026 after starting four PostgreSQL containers and four Spring Boot services.

| Step | Expected result | Verified result |
|---|---:|---:|
| Register passenger | 201 | Pass |
| Register driver | 201 | Pass |
| Login passenger | 200 | Pass |
| Login driver | 200 | Pass |
| Create driver profile | 201 | Pass |
| Add vehicle | 201 | Pass |
| Update driver location | 200 | Pass |
| Set driver available | 200 | Pass |
| Create fare estimate | 201 | Pass |
| Create and assign ride | 201 | Pass |
| Accept ride | 200 | Pass |
| Start ride | 200 | Pass |
| Complete ride | 200 | Pass |
| View final fare | 200 | Pass |
| Record simulated payment | 201 | Pass |
| Retrieve receipt | 200 | Pass |

Verified ride ID: `f54245a8-2a41-49bd-a1ee-8efe044dcdff`  
Verified payment ID: `acea6638-5a42-4dde-be9d-9149aa9cc350`

## 2. Services and ports

| Service | URL | Database | Database host port |
|---|---|---|---:|
| Account | `http://localhost:8081` | `account_db` | 55431 |
| Driver & Vehicle | `http://localhost:8082` | `driver_db` | 55432 |
| Ride Management | `http://localhost:8083` | `ride_db` | 55433 |
| Fare & Payment | `http://localhost:8084` | `payment_db` | 55434 |

Health and Swagger links:

| Service | Health | Swagger UI |
|---|---|---|
| Account | `http://localhost:8081/actuator/health` | `http://localhost:8081/swagger-ui.html` |
| Driver | `http://localhost:8082/actuator/health` | `http://localhost:8082/swagger-ui.html` |
| Ride | `http://localhost:8083/actuator/health` | `http://localhost:8083/swagger-ui.html` |
| Fare | `http://localhost:8084/actuator/health` | `http://localhost:8084/swagger-ui.html` |

Each health endpoint should return:

```json
{"status":"UP"}
```

## 3. Start the databases

Open PowerShell in `D:\RideLink`:

```powershell
cd D:\RideLink
docker compose up -d
docker compose ps
```

Wait until all four rows show `healthy`. The root Compose file uses the local database password `ridelink-local-only` unless an environment override is supplied.

## 4. Start the services

Use the same JWT secret and service token in all four terminals. The values below are local demonstration values only.

### Terminal 1 — Account Service

```powershell
cd D:\RideLink\account-service
$env:ACCOUNT_DB_URL = "jdbc:postgresql://localhost:55431/account_db"
$env:ACCOUNT_DB_USERNAME = "postgres"
$env:ACCOUNT_DB_PASSWORD = "ridelink-local-only"
$env:JWT_SECRET = "ridelink-local-jwt-secret-2026-demo-only"
$env:SERVICE_TOKEN = "ridelink-local-service-token-2026-demo-only"
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
$env:BOOTSTRAP_ADMIN_PASSWORD = "AdminPass1"
$env:BOOTSTRAP_ADMIN_FULL_NAME = "RideLink Administrator"
$env:BOOTSTRAP_ADMIN_PHONE = "+94770000000"
.\mvnw.cmd spring-boot:run
```

### Terminal 2 — Driver & Vehicle Service

```powershell
cd D:\RideLink\driver-vehicle-service
$env:DRIVER_DB_URL = "jdbc:postgresql://localhost:55432/driver_db"
$env:DRIVER_DB_USERNAME = "postgres"
$env:DRIVER_DB_PASSWORD = "ridelink-local-only"
$env:JWT_SECRET = "ridelink-local-jwt-secret-2026-demo-only"
$env:SERVICE_TOKEN = "ridelink-local-service-token-2026-demo-only"
$env:ACCOUNT_SERVICE_URL = "http://localhost:8081"
.\mvnw.cmd spring-boot:run
```

### Terminal 3 — Fare & Payment Service

```powershell
cd D:\RideLink\fare-payment-service
$env:PAYMENT_DB_URL = "jdbc:postgresql://localhost:55434/payment_db"
$env:PAYMENT_DB_USERNAME = "postgres"
$env:PAYMENT_DB_PASSWORD = "ridelink-local-only"
$env:JWT_SECRET = "ridelink-local-jwt-secret-2026-demo-only"
$env:SERVICE_TOKEN = "ridelink-local-service-token-2026-demo-only"
$env:RIDE_SERVICE_URL = "http://localhost:8083"
.\mvnw.cmd spring-boot:run
```

### Terminal 4 — Ride Management Service

```powershell
cd D:\RideLink\ride-management-service
$env:RIDE_DB_URL = "jdbc:postgresql://localhost:55433/ride_db"
$env:RIDE_DB_USERNAME = "postgres"
$env:RIDE_DB_PASSWORD = "ridelink-local-only"
$env:JWT_SECRET = "ridelink-local-jwt-secret-2026-demo-only"
$env:SERVICE_TOKEN = "ridelink-local-service-token-2026-demo-only"
$env:DRIVER_SERVICE_URL = "http://localhost:8082"
$env:FARE_SERVICE_URL = "http://localhost:8084"
.\mvnw.cmd spring-boot:run
```

Recommended start order: Account, Driver, Fare, then Ride. Wait for each application to show `Started ...Application` before continuing.

## 5. Import Postman files

Import these five files:

1. `D:\RideLink\postman\RideLink-Local.postman_environment.json`
2. `D:\RideLink\postman\RideLink-Account-Service.postman_collection.json`
3. `D:\RideLink\postman\RideLink-Driver-Service.postman_collection.json`
4. `D:\RideLink\postman\RideLink-Fare-Service.postman_collection.json`
5. `D:\RideLink\postman\RideLink-Ride-Service.postman_collection.json`

Select the **RideLink Local** environment. Set these environment values:

| Variable | Value |
|---|---|
| `accountBaseUrl` | `http://localhost:8081` |
| `driverBaseUrl` | `http://localhost:8082` |
| `rideBaseUrl` | `http://localhost:8083` |
| `fareBaseUrl` | `http://localhost:8084` |
| `serviceToken` | `ridelink-local-service-token-2026-demo-only` |
| `adminEmail` | `admin@example.com` |
| `adminPassword` | `AdminPass1` |

Do not manually fill the JWTs or generated IDs. The numbered requests save them automatically.

## 6. Correct successful workflow

Run individual requests in this exact order. Do not use **Run collection** for all Account requests at the beginning, because Account request 09 intentionally suspends the passenger.

1. Account requests `01` to `06`.
2. Driver requests `01` to `06`.
3. Fare requests `01` to `03`.
4. Ride requests `01` to `08`.
5. Fare requests `04` to `06`.

The automatically saved variables are:

- `passengerAccountId`
- `driverAccountId`
- `passengerToken`
- `driverToken`
- `adminToken`
- `driverId`
- `estimateId`
- `rideId`
- `paymentId`

If registration returns `409` because the demo email already exists, either change the two email addresses in Account requests 02–05 or reset only the local demonstration data with `docker compose down -v` and start again. The `-v` command deletes the four local database volumes, so use it only when you intentionally want a clean demo.

## 7. Account Service endpoints

### Public status

`GET {{accountBaseUrl}}/api/v1/status` — expected `200`.

### Register passenger

`POST {{accountBaseUrl}}/api/v1/accounts/passengers` — expected `201`.

```json
{
  "email": "passenger.demo@example.com",
  "password": "PassengerPass1",
  "fullName": "Demo Passenger",
  "phone": "+94771111111"
}
```

The server assigns the `PASSENGER` role.

### Register driver

`POST {{accountBaseUrl}}/api/v1/accounts/drivers` — expected `201`.

```json
{
  "email": "driver.demo@example.com",
  "password": "DriverPass1",
  "fullName": "Demo Driver",
  "phone": "+94772222222"
}
```

The server assigns the `DRIVER` role.

### Login

`POST {{accountBaseUrl}}/api/v1/auth/login` — expected `200`.

```json
{
  "email": "passenger.demo@example.com",
  "password": "PassengerPass1"
}
```

Repeat with `driver.demo@example.com` / `DriverPass1`, and with `{{adminEmail}}` / `{{adminPassword}}`. Save `accessToken` as the relevant environment token.

### View own profile

`GET {{accountBaseUrl}}/api/v1/accounts/me` — expected `200`.

Header: `Authorization: Bearer {{passengerToken}}`

### Update own profile

`PATCH {{accountBaseUrl}}/api/v1/accounts/me` — expected `200`.

Header: `Authorization: Bearer {{passengerToken}}`

```json
{
  "fullName": "Updated Demo Passenger",
  "phone": "+94773333333"
}
```

### Suspend or reactivate account

`PATCH {{accountBaseUrl}}/api/v1/accounts/{{passengerAccountId}}/status` — expected `200`.

Header: `Authorization: Bearer {{adminToken}}`

```json
{"status":"SUSPENDED"}
```

The suspended passenger login should return `403`. Reactivate at the end:

```json
{"status":"ACTIVE"}
```

### Internal account summary

`GET {{accountBaseUrl}}/internal/v1/accounts/{{driverAccountId}}/summary` — expected `200`.

Header: `X-Service-Token: {{serviceToken}}`

## 8. Driver & Vehicle Service endpoints

### Public status

`GET {{driverBaseUrl}}/api/v1/status` — expected `200`.

### Create driver profile

`POST {{driverBaseUrl}}/api/v1/drivers/me/profile` — expected `201`.

Header: `Authorization: Bearer {{driverToken}}`

```json
{
  "serviceArea": "COLOMBO",
  "locationName": "Bambalapitiya",
  "latitude": 6.8935,
  "longitude": 79.8552
}
```

### Add or replace vehicle

`PUT {{driverBaseUrl}}/api/v1/drivers/me/vehicle` — expected `201` when first created and `200` when replaced.

Header: `Authorization: Bearer {{driverToken}}`

```json
{
  "registrationNumber": "CAB-1234",
  "vehicleType": "CAR",
  "manufacturer": "Toyota",
  "model": "Aqua",
  "colour": "White",
  "seatCapacity": 4,
  "status": "ACTIVE"
}
```

### Update location

`PATCH {{driverBaseUrl}}/api/v1/drivers/me/location` — expected `200`.

Header: `Authorization: Bearer {{driverToken}}`

```json
{
  "serviceArea": "COLOMBO",
  "locationName": "Wellawatte",
  "latitude": 6.8747,
  "longitude": 79.8605
}
```

### Update availability

`PATCH {{driverBaseUrl}}/api/v1/drivers/me/availability` — expected `200`.

Header: `Authorization: Bearer {{driverToken}}`

```json
{"availability":"AVAILABLE"}
```

Allowed manual values are `AVAILABLE` and `OFFLINE`. The driver must have an active vehicle and location before becoming available.

### View own driver profile

`GET {{driverBaseUrl}}/api/v1/drivers/me` — expected `200`.

Header: `Authorization: Bearer {{driverToken}}`

### Find eligible drivers

`GET {{driverBaseUrl}}/internal/v1/drivers/eligible?serviceArea=COLOMBO&seatCount=2` — expected `200`.

Header: `X-Service-Token: {{serviceToken}}`

### Reserve a driver

`POST {{driverBaseUrl}}/internal/v1/drivers/{{driverId}}/reservations` — expected `201` for a new reservation or `200` for an idempotent replay.

Header: `X-Service-Token: {{serviceToken}}`

```json
{"rideId":"{{rideId}}"}
```

Ride Service normally performs this call automatically.

### Release a driver

`DELETE {{driverBaseUrl}}/internal/v1/drivers/{{driverId}}/reservations/{{rideId}}` — expected `204`.

Header: `X-Service-Token: {{serviceToken}}`

## 9. Fare & Payment Service endpoints

### Public status

`GET {{fareBaseUrl}}/api/v1/status` — expected `200`.

### Create fare estimate

`POST {{fareBaseUrl}}/api/v1/fares/estimates` — expected `201`.

Header: `Authorization: Bearer {{passengerToken}}`

```json
{
  "pickup": "Wellawatte",
  "destination": "Colombo Fort",
  "distanceKm": 10.50
}
```

Expected amount: `1090.00` LKR because `250 + (10.50 × 80) = 1090`.

### Validate estimate internally

`GET {{fareBaseUrl}}/internal/v1/fares/estimates/{{estimateId}}` — expected `200`.

Header: `X-Service-Token: {{serviceToken}}`

### Create final fare internally

`POST {{fareBaseUrl}}/internal/v1/fares/final` — called automatically by Ride Service when a ride completes.

Header: `X-Service-Token: {{serviceToken}}`

This internal operation creates one final fare per ride and is idempotent.

### View final fare

`GET {{fareBaseUrl}}/api/v1/fares/rides/{{rideId}}` — expected `200` after ride completion.

Header: `Authorization: Bearer {{passengerToken}}`

### Record simulated payment

`POST {{fareBaseUrl}}/api/v1/payments` — expected `201`.

Headers:

- `Authorization: Bearer {{passengerToken}}`
- `Idempotency-Key: {{$guid}}`

```json
{
  "rideId": "{{rideId}}",
  "simulationOutcome": "SUCCESS",
  "methodLabel": "SIMULATED_CARD"
}
```

Expected payment status: `PAID`. This is a simulation and does not process real money.

### Retrieve receipt

`GET {{fareBaseUrl}}/api/v1/payments/{{paymentId}}/receipt` — expected `200`.

Header: `Authorization: Bearer {{passengerToken}}`

## 10. Ride Management Service endpoints

### Public status

`GET {{rideBaseUrl}}/api/v1/status` — expected `200`.

### Create and assign ride

`POST {{rideBaseUrl}}/api/v1/rides` — expected `201` with status `ASSIGNED`.

Headers:

- `Authorization: Bearer {{passengerToken}}`
- `Idempotency-Key: {{$guid}}`

```json
{
  "pickup": "Wellawatte",
  "destination": "Colombo Fort",
  "serviceArea": "COLOMBO",
  "distanceKm": 10.50,
  "seatCount": 2,
  "fareEstimateId": "{{estimateId}}"
}
```

### View one ride

`GET {{rideBaseUrl}}/api/v1/rides/{{rideId}}` — expected `200` for the passenger, assigned driver, or admin.

Header: `Authorization: Bearer {{passengerToken}}`

### List rides

`GET {{rideBaseUrl}}/api/v1/rides?page=0&size=20` — expected `200`.

Header: `Authorization: Bearer {{passengerToken}}`

Passengers and drivers see their rides; an administrator can see all rides.

### Accept ride

`POST {{rideBaseUrl}}/api/v1/rides/{{rideId}}/accept` — expected `200` and status `ACCEPTED`.

Header: `Authorization: Bearer {{driverToken}}`

### Start ride

`POST {{rideBaseUrl}}/api/v1/rides/{{rideId}}/start` — expected `200` and status `IN_PROGRESS`.

Header: `Authorization: Bearer {{driverToken}}`

### Complete ride

`POST {{rideBaseUrl}}/api/v1/rides/{{rideId}}/complete` — expected `200` and status `COMPLETED`.

Header: `Authorization: Bearer {{driverToken}}`

Completion creates the final fare and releases the reserved driver.

### Cancel ride

`POST {{rideBaseUrl}}/api/v1/rides/{{rideId}}/cancel` — expected `200` before the ride is in progress.

Header: `Authorization: Bearer {{passengerToken}}`

```json
{"reason":"Passenger changed plans"}
```

### Internal ride summary

`GET {{rideBaseUrl}}/internal/v1/rides/{{rideId}}/summary` — expected `200`.

Header: `X-Service-Token: {{serviceToken}}`

## 11. Negative tests

### Independent negative requests

| Request | Expected result |
|---|---|
| Account internal summary with `wrong-token` | `401` |
| Driver eligible search with `wrong-token` | `401` |
| Fare estimate validation with `wrong-token` | `401` |
| Ride internal summary with `wrong-token` | `401` |
| Passenger changes driver availability | `403` |
| Driver location with blank names and latitude `95` | `400` |
| Fare estimate with `distanceKm: 0` | `400` |
| Passenger accepts a ride | `403` |

### Stateful invalid-transition test

Run this exact order while the driver is `AVAILABLE`:

1. Fare: **Negative setup - create another estimate**. This saves `negativeEstimateId`.
2. Ride: **Negative setup - create another assigned ride**. This saves `negativeRideId`.
3. Ride: **Negative - driver starts before accepting**. Expect `409 INVALID_RIDE_TRANSITION`.
4. Fare: **Negative - payment before ride completion**. Expect `422 RIDE_NOT_COMPLETED`.
5. Ride: **Negative cleanup - cancel second ride**. Expect `200` and the driver is released.

### No eligible driver test

1. Change the availability request body to `{"availability":"OFFLINE"}` and run it with the driver JWT.
2. Run Fare **Negative setup - create another estimate** again to obtain a fresh estimate ID.
3. Run Ride **Negative - no eligible driver**.
4. Expect `409 NO_AVAILABLE_DRIVER`.
5. Set the driver back to `AVAILABLE` for later demonstrations.

## 12. Automated code tests

Run from each service folder:

```powershell
cd D:\RideLink\account-service
.\mvnw.cmd clean test

cd D:\RideLink\driver-vehicle-service
.\mvnw.cmd clean test

cd D:\RideLink\ride-management-service
.\mvnw.cmd clean test

cd D:\RideLink\fare-payment-service
.\mvnw.cmd clean test
```

Expected total: 32 passing tests with zero failures and errors.

Repository check:

```powershell
cd D:\RideLink
.\scripts\pre-pr-check.ps1 -BaseRef origin/develop
```

## 13. Evidence to capture

Capture these screenshots for the report or viva:

1. `docker compose ps` showing four separate healthy PostgreSQL containers.
2. All four `/actuator/health` responses showing `UP`.
3. All four Swagger UI home pages.
4. Account registration and login responses, with JWT text hidden if the screenshot is shared.
5. Driver profile, vehicle, location, and `AVAILABLE` response.
6. Fare estimate showing LKR `1090.00` for 10.50 km.
7. Ride response at `ASSIGNED`, `ACCEPTED`, `IN_PROGRESS`, and `COMPLETED`.
8. Payment response showing `PAID` and the receipt response.
9. At least four negative cases with their expected status and error code.
10. Maven summaries and GitHub Actions showing passing tests.

Use Postman's **Save Response → Save as example** or take a screenshot with the request name, URL, status, and response body visible. Do not expose complete JWTs, passwords, database passwords, or service tokens.

## 14. Stop the system

Stop each Spring Boot terminal with `Ctrl+C`. Then stop the databases without deleting data:

```powershell
cd D:\RideLink
docker compose down
```

To start later with the same data, run `docker compose up -d` again. Avoid `docker compose down -v` unless a clean database reset is intentional.


The response contains only `accountId`, `role`, and `status`. A wrong service token should return `401`.
