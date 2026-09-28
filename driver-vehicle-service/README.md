# RideLink Driver & Vehicle Service

Owner: Hirula Abesignha, Member 2<br>
Service port: `8082`<br>
Owned database: `driver_db`

This Spring Boot service owns the operational driver profile, vehicle, availability, simulated location and temporary ride reservation. It never reads another service's database.

## What this service implements

- Driver profile creation after Account Service confirms an active `DRIVER` account
- View the signed-in driver's own profile and vehicle
- Create or replace one current vehicle
- Change availability between `OFFLINE` and `AVAILABLE`
- Update the driver's service area and simulated location
- Find eligible drivers for Ride Service
- Atomically reserve an available driver for one ride
- Safely replay the same reservation and release request
- JWT role protection for driver endpoints
- `X-Service-Token` protection for internal endpoints
- PostgreSQL schema management with Flyway
- Swagger UI, consistent API errors, correlation IDs and health checks

## Package guide

| Package | Purpose |
|---|---|
| `api.dto` | Validated request and response objects |
| `api.error` | Contract error responses and exception handling |
| `client` | Account Service communication and retry logic |
| `config` | JWT security, service token security, Swagger and HTTP client setup |
| `controller` | Public driver and internal Ride Service endpoints |
| `domain` | Driver and vehicle database entities and enums |
| `repository` | Database queries, including atomic reservation updates |
| `service` | Business rules and transaction boundaries |

## Business rules

1. A new profile always starts `OFFLINE`.
2. The JWT subject must belong to an active account with role `DRIVER`.
3. A driver needs an active vehicle and a location before becoming `AVAILABLE`.
4. A driver can request only `AVAILABLE` or `OFFLINE`. Ride Service alone can create `RESERVED`.
5. A reserved driver cannot replace the vehicle or manually change availability.
6. Eligibility requires the same service area, `AVAILABLE` state, an active vehicle and enough seats.
7. Reservation uses one conditional database update, so two rides cannot reserve the same available driver.
8. Repeating the same driver and ride reservation returns the existing reservation.
9. Release succeeds only for the matching ride. Repeating a completed release is safe.

## API summary

| Method | Path | Access | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/status` | Public | Simple service status |
| `POST` | `/api/v1/drivers/me/profile` | DRIVER JWT | Create driver profile |
| `GET` | `/api/v1/drivers/me` | DRIVER JWT | View own profile and vehicle |
| `PUT` | `/api/v1/drivers/me/vehicle` | DRIVER JWT | Create or replace vehicle |
| `PATCH` | `/api/v1/drivers/me/availability` | DRIVER JWT | Go available or offline |
| `PATCH` | `/api/v1/drivers/me/location` | DRIVER JWT | Update simulated location |
| `GET` | `/internal/v1/drivers/eligible` | Service token | Find eligible drivers |
| `POST` | `/internal/v1/drivers/{driverId}/reservations` | Service token | Reserve a driver |
| `DELETE` | `/internal/v1/drivers/{driverId}/reservations/{rideId}` | Service token | Release a reservation |

Swagger UI: `http://localhost:8082/swagger-ui.html`<br>
OpenAPI JSON: `http://localhost:8082/v3/api-docs`<br>
Health: `http://localhost:8082/actuator/health`

## Required environment variables

```text
DRIVER_DB_URL          JDBC URL for this service's own PostgreSQL database
DRIVER_DB_USERNAME     database username
DRIVER_DB_PASSWORD     database password
JWT_SECRET             same 32+ character HS256 secret used by Account Service
SERVICE_TOKEN          same 32+ character token used for internal service calls
ACCOUNT_SERVICE_URL    Account Service address, normally http://localhost:8081
```

Never commit the real values. The root `.env.example` contains harmless placeholders.

## Quick local setup with Docker

From this directory:

```powershell
$env:DRIVER_DB_PASSWORD = "choose-a-local-password"
docker compose up -d

$env:DRIVER_DB_URL = "jdbc:postgresql://localhost:55432/driver_db"
$env:DRIVER_DB_USERNAME = "postgres"
$env:JWT_SECRET = "replace-with-a-random-jwt-secret-32-chars-minimum"
$env:SERVICE_TOKEN = "replace-with-a-random-service-token-32-chars-minimum"
$env:ACCOUNT_SERVICE_URL = "http://localhost:8081"
.\mvnw.cmd spring-boot:run
```

Flyway creates the tables automatically. Docker stores the database in the named `driver-db-data` volume.

To stop the local database:

```powershell
docker compose down
```

## Run without Docker

Create a PostgreSQL database named `driver_db`, set the same environment variables with the correct JDBC URL, and run:

```powershell
.\mvnw.cmd spring-boot:run
```

## Run the tests

```powershell
.\mvnw.cmd clean test
```

The test profile uses its own H2 database in PostgreSQL compatibility mode. Flyway runs the real migration and Hibernate validates it. The test suite covers driver role checks, request validation, profile and vehicle rules, availability rules, internal token security, eligibility, reservation replay, reservation conflicts, release mismatch, repeated release and Account Service retry.

## Demonstration order

1. Start Account Service and this service.
2. Register and sign in as a driver through Account Service.
3. Put the driver JWT in the Postman `driverToken` variable.
4. Create the operational driver profile.
5. Add an active vehicle.
6. Change availability to `AVAILABLE`.
7. Find eligible drivers using the service token.
8. Reserve the returned driver for a ride.
9. Show that a second ride receives `409 DRIVER_NOT_AVAILABLE`.
10. Release the matching reservation.
11. Show that a wrong service token receives `401 INVALID_SERVICE_TOKEN`.

The ready-to-import requests are in `postman/RideLink-Driver-Service.postman_collection.json`.

## Integration notes for other members

- Account Service must implement `GET /internal/v1/accounts/{accountId}/summary` and return `accountId`, `role` and `status`.
- Ride Service must send `X-Service-Token` to every `/internal/v1/**` endpoint.
- Ride Service should store both `driverId` and the returned `accountId`.
- Ride Service should call release if saving the assigned ride fails after reservation.
- Service area comparison is case insensitive. Eligible drivers are returned by `driverId` in ascending order.
- Connect timeout to Account Service is 2 seconds, read timeout is 3 seconds, and timeout or server failure is retried once.
