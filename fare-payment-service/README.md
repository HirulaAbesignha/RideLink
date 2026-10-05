# Fare & Payment Service

Java 17 / Spring Boot microservice owned by Member 4. It owns the `payment_db` database and never reads or writes another service's database. Payment is simulated; the service does not process real money.

## Responsibilities

- Create fare estimates using `LKR 250.00 + (distanceKm × LKR 80.00)`, rounded to two decimals with `HALF_UP`.
- Keep estimates for 30 minutes and expose estimate validation to Ride Service.
- Create one idempotent final fare per ride.
- Record simulated success/failure payments and issue receipts only for successful payments.
- Verify ride state and participant details through Ride Service's internal REST API.

## Configure and run

Requirements: Java 17 and PostgreSQL. Start the local database with `docker compose up -d`, then set local environment variables in PowerShell. Use random values of at least 32 characters for both secrets, matching the values used by the other services:

```powershell
$env:PAYMENT_DB_PASSWORD = "your-local-database-password"
$env:PAYMENT_DB_URL = "jdbc:postgresql://localhost:55434/payment_db"
$env:JWT_SECRET = "your-local-random-secret-at-least-32-characters"
$env:SERVICE_TOKEN = "your-shared-local-service-token-at-least-32-characters"
$env:RIDE_SERVICE_URL = "http://localhost:8083"
.\mvnw.cmd spring-boot:run
```

The database URL and username default to `jdbc:postgresql://localhost:5432/payment_db` and `postgres`; override them with `PAYMENT_DB_URL` and `PAYMENT_DB_USERNAME` when needed. Flyway creates the tables in this service's own database.

Start Account (8081), Driver (8082), and Ride (8083) before demonstrating payment. Swagger UI: <http://localhost:8084/swagger-ui.html>; OpenAPI JSON: <http://localhost:8084/v3/api-docs>.

## Main endpoints

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/fares/estimates` | Passenger, driver, admin JWT |
| GET | `/internal/v1/fares/estimates/{estimateId}` | `X-Service-Token` |
| POST | `/internal/v1/fares/final` | `X-Service-Token` |
| GET | `/api/v1/fares/rides/{rideId}` | Ride participant or admin JWT |
| POST | `/api/v1/payments` | Passenger JWT and `Idempotency-Key` UUID |
| GET | `/api/v1/payments/{paymentId}/receipt` | Receipt owner or admin JWT |

Example estimate body:

```json
{"pickup":"Wellawatte","destination":"Colombo Fort","distanceKm":10.50}
```

Example simulated payment body (only after Ride Service reports `COMPLETED`):

```json
{"rideId":"b7e96cc7-9b07-4c15-a313-317b443bafc7","simulationOutcome":"SUCCESS","methodLabel":"SIMULATED_CARD"}
```

For local requests, obtain the JWT from Account Service and send `Authorization: Bearer <token>`. Internal requests use `X-Service-Token`; never commit either secret. The payment service verifies that the JWT passenger owns the completed ride. Repeating the same payment body with its original idempotency key returns the saved payment; reusing the key with a different body returns `409`.

## Run tests

```powershell
.\mvnw.cmd test
```
