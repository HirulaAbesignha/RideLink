# RideLink

RideLink is the backend for the IT3130 Application Development group assignment. It uses four Java and Spring Boot microservices. Each service owns a separate PostgreSQL database and communicates with the other services only through documented REST APIs.

## Services

| Directory | Owner | Port | Database | Responsibility |
|---|---|---:|---|---|
| `account-service` | Member 1 | 8081 | `account_db` | Accounts, login, JWTs, roles and profiles |
| `driver-vehicle-service` | Hirula Abesignha, Member 2 | 8082 | `driver_db` | Drivers, vehicles, availability and location |
| `ride-management-service` | Member 3 | 8083 | `ride_db` | Ride requests, driver assignment and lifecycle |
| `fare-payment-service` | Member 4 | 8084 | `payment_db` | Estimates, final fares, simulated payments and receipts |

Add every member's full name and student ID to the final report and contribution log before submission.

## Technology

- Java 17 and Spring Boot 4.0.8
- Maven Wrapper
- Spring Web MVC, Validation, Security and Data JPA
- PostgreSQL with Flyway migrations
- HS256 JWTs for users and a shared service token for internal APIs
- Swagger UI and Postman for demonstration
- JUnit, MockMvc and H2 in PostgreSQL compatibility mode for automated tests

No service reads another service's database. Cross-service references are UUID values and are checked through internal REST endpoints.

## Start the databases

Docker Compose creates four separate PostgreSQL containers and four separate named volumes:

```powershell
docker compose up -d
docker compose ps
```

The host ports are 55431, 55432, 55433 and 55434. Copy `.env.example` values into your terminal or IDE environment. Use the same `JWT_SECRET` and `SERVICE_TOKEN` in all four services.

If Docker is unavailable, create `account_db`, `driver_db`, `ride_db` and `payment_db` in a local PostgreSQL installation and change the four database URLs.

## Start the services

Open four PowerShell terminals. Set the environment values in each terminal, then start the services in this order:

1. Account Service
2. Driver & Vehicle Service
3. Fare & Payment Service
4. Ride Management Service

Run a service from its directory:

```powershell
.\mvnw.cmd spring-boot:run
```

| Service | Health | Swagger UI |
|---|---|---|
| Account | http://localhost:8081/actuator/health | http://localhost:8081/swagger-ui.html |
| Driver | http://localhost:8082/actuator/health | http://localhost:8082/swagger-ui.html |
| Ride | http://localhost:8083/actuator/health | http://localhost:8083/swagger-ui.html |
| Fare | http://localhost:8084/actuator/health | http://localhost:8084/swagger-ui.html |

## Run all tests

Run this command inside each service directory:

```powershell
.\mvnw.cmd clean test
```

Before a pull request, run the repository checker from the root:

```powershell
.\scripts\pre-pr-check.ps1 -BaseRef origin/develop
```

The GitHub Actions workflows repeat the repository checks and all four service test suites.

## Demonstration flow

1. Register and log in a passenger and driver through Account Service.
2. Create the driver profile and vehicle, then set the driver to `AVAILABLE`.
3. Create a fare estimate as the passenger.
4. Create a ride with the estimate ID. Ride Service validates the estimate and reserves an eligible driver.
5. Accept, start and complete the ride as the assigned driver.
6. Record a simulated successful payment as the passenger.
7. Retrieve the receipt.

Import the files in `postman/` and run their numbered requests. The API decisions and negative cases are documented in `docs/api-contracts/API_CONTRACT.md`.

## Repository documents

- `CONTRIBUTING.md` explains branches, commits and pull requests.
- `docs/api-contracts/API_CONTRACT.md` is the shared API baseline.
- `docs/CONTRIBUTION_LOG.md` records individual work and evidence.
- `docs/architecture/` contains the architecture and request flow.
- `docs/evidence/` stores non-sensitive test and CI evidence.
- `postman/` contains the local environment and service collections.

## Submission checklist

- Replace remaining member name and student ID placeholders.
- Confirm all four GitHub Actions service jobs pass on the final pull request.
- Demonstrate the full flow and required negative cases in Postman.
- Make sure Swagger, the API contract, the final report and the code describe the same endpoints.
- Merge the assessed commit into `main` and create the agreed release tag.
- Confirm no passwords, JWTs, service tokens, database volumes or build output are committed.
