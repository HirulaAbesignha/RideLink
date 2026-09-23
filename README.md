# RideLink

Backend microservices group assignment for IT3130 Application Development.

## Important rules

- All four services use Java and Spring Boot.
- Each service owns a separate database.
- Services communicate through documented APIs, never through another service's database.
- Swagger UI and Postman are the demonstration clients. No frontend is required.
- Do not commit passwords, tokens, connection strings with secrets, build output or dependency folders.
- Every member works through their own Git identity, feature branches, pull requests and peer reviews.

## Services

| Directory | Owner | Port | Database | Main responsibility |
|---|---|---:|---|---|
| `account-service` | Member 1 - add name/ID | 8081 | `account_db` | Accounts, login, JWTs, roles and profiles |
| `driver-vehicle-service` | Member 2 - add name/ID | 8082 | `driver_db` | Vehicles, availability, service area and simulated location |
| `ride-management-service` | Member 3 - add name/ID | 8083 | `ride_db` | Ride requests, assignment and lifecycle |
| `fare-payment-service` | Member 4 - add name/ID | 8084 | `payment_db` | Fare estimates, final fares, simulated payments and receipts |

## Technology baseline

- Java 17
- Spring Boot 4.0.8
- Maven Wrapper
- Spring Web MVC, Validation, Security and Data JPA
- PostgreSQL and Flyway
- springdoc OpenAPI/Swagger UI
- JUnit and an in-memory H2 test profile

The generated starter applications are deliberately small. Each owner must implement, test and document their assigned business service.

If your laboratory sessions prescribed a specific Java or Spring Boot version, update all four services together before business development begins and record that decision in the report.

## First-time setup

1. Install Java 17 and PostgreSQL.
2. Clone this repository.
3. Create the four databases shown above using PostgreSQL or pgAdmin.
4. Set the database password environment variable for the service you are running.
5. Run its tests before writing business code.

Example for the Driver & Vehicle Service in PowerShell:

```powershell
cd driver-vehicle-service
$env:DRIVER_DB_PASSWORD = "your-local-postgres-password"
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Then open `http://localhost:8082/swagger-ui.html`.

## Branch workflow

The recommended branches are `main`, `develop` and short-lived feature branches.

```text
main
  └── develop
        ├── feature/account-registration
        ├── feature/driver-profile
        ├── feature/ride-lifecycle
        └── feature/fare-estimation
```

For every change:

1. Pull the latest `develop`.
2. Create a feature branch.
3. Make small, meaningful commits.
4. Push the branch and open a pull request into `develop`.
5. Ask a different member to review it.
6. Merge only after the CI checks pass.

Do not push business features directly to `main` or `develop`.

## Documentation and shared evidence

- `CONTRIBUTING.md`: mandatory beginner workflow for branches, commits, pull requests, reviews and contribution evidence.
- `docs/TEAM_START_HERE.md`: beginner setup and first meeting checklist.
- `docs/api-contracts/API_CONTRACT.md`: decisions that must be agreed before integration.
- `docs/CONTRIBUTION_LOG.md`: member, PR, review and final contribution record.
- `docs/architecture/`: architecture and sequence diagrams.
- `docs/evidence/`: non-sensitive CI, test and integration evidence.
- `postman/`: exported Postman collection and example environment.

## Submission readiness

The assessed version must be an integrated commit on `main` with a release tag. All four services must build and test in CI. The root README, final report, Swagger documentation and Postman collection must match the tagged code.

