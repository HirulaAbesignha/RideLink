# Ride Management Service

This Java and Spring Boot service owns `ride_db`. It manages ride requests, driver assignment and the ride lifecycle. It obtains fare and driver information through REST APIs and never reads another service's database.

## Responsibilities

- Validate a passenger's fare estimate through Fare Service.
- Save the ride before searching for a driver.
- Find and reserve an eligible driver through Driver Service.
- Enforce passenger, driver and administrator access rules.
- Manage `REQUESTED`, `ASSIGNED`, `ACCEPTED`, `IN_PROGRESS`, `COMPLETED` and `CANCELLED` states.
- Create the final fare and release the driver when a ride completes.
- Support idempotent ride creation with `Idempotency-Key`.

## Configure and run

Requirements: Java 17 and PostgreSQL. Start the local database and service with:

```powershell
docker compose up -d
$env:RIDE_DB_URL = "jdbc:postgresql://localhost:55433/ride_db"
$env:RIDE_DB_USERNAME = "postgres"
$env:RIDE_DB_PASSWORD = "ridelink-local-only"
$env:JWT_SECRET = "replace-with-a-random-jwt-secret-32-chars-minimum"
$env:SERVICE_TOKEN = "replace-with-a-random-service-token-32-chars-minimum"
$env:DRIVER_SERVICE_URL = "http://localhost:8082"
$env:FARE_SERVICE_URL = "http://localhost:8084"
.\mvnw.cmd spring-boot:run
```

Use the same JWT secret and service token as the connected services. Swagger UI is available at <http://localhost:8083/swagger-ui.html> and health at <http://localhost:8083/actuator/health>.

## Main endpoints

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/v1/rides` | Passenger JWT and `Idempotency-Key` |
| GET | `/api/v1/rides/{rideId}` | Ride participant or admin JWT |
| GET | `/api/v1/rides` | Authenticated user's rides, or all rides for admin |
| POST | `/api/v1/rides/{rideId}/accept` | Assigned driver JWT |
| POST | `/api/v1/rides/{rideId}/start` | Assigned driver JWT |
| POST | `/api/v1/rides/{rideId}/complete` | Assigned driver JWT |
| POST | `/api/v1/rides/{rideId}/cancel` | Ride passenger JWT |
| GET | `/internal/v1/rides/{rideId}/summary` | `X-Service-Token` |

When no driver is available, the service keeps the ride as `REQUESTED` and returns `409 NO_AVAILABLE_DRIVER`. Repeating the same creation request with the same key returns that saved ride.

## Run tests

```powershell
.\mvnw.cmd clean test
```

The tests run all Flyway migrations, validate the schema and cover assignment, idempotency, authorization, lifecycle completion and dependency failure behavior.
