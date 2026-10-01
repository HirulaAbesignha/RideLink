# RideLink Account Service

Owner: Shasrika (Member 1)<br>
Service port: `8081`<br>
Owned database: `account_db`

This Spring Boot service owns RideLink accounts, credentials, roles, account status and basic contact profiles. Other services use its small internal summary endpoint instead of reading `account_db`.

## What this service implements

- Passenger and driver registration with server assigned roles
- BCrypt password hashing and strong password validation
- Login with signed HS256 JWT access tokens
- Passenger, driver and admin role claims
- View and update the signed in account profile
- Admin account suspension and reactivation with an audit record
- Internal account summaries without personal data
- JWT protection for account endpoints
- `X-Service-Token` protection for service communication
- PostgreSQL schema management with Flyway
- Swagger UI, consistent API errors, correlation IDs and health checks

## Package guide

| Package | Purpose |
|---|---|
| `api.dto` | Validated request and response objects |
| `api.error` | Contract error responses and exception handling |
| `config` | JWT, service token, admin bootstrap and Swagger setup |
| `controller` | Public account, login and internal service endpoints |
| `domain` | Account and status audit database entities |
| `repository` | Account and audit database access |
| `service` | Registration, login, token and account business rules |

## Business and security rules

1. The server assigns `PASSENGER` or `DRIVER`; registration cannot supply a role.
2. Emails are trimmed, stored in lowercase and kept unique.
3. Passwords must be 8 to 72 characters and contain uppercase, lowercase and numeric characters.
4. Only BCrypt hashes are stored. Passwords and hashes are never returned.
5. JWTs use HS256, contain `iss`, `sub`, `role`, `iat`, `exp` and `jti`, and expire after 60 minutes.
6. A suspended account cannot obtain a new token. A token issued before suspension remains valid until its normal expiry.
7. Only an active admin can change account status. Each actual status change creates an audit record.
8. An account can read and update only the profile identified by its JWT subject.
9. Internal summaries return only `accountId`, `role` and `status`.

## API summary

| Method | Path | Access | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/status` | Public | Simple service status |
| `POST` | `/api/v1/accounts/passengers` | Public | Register a passenger |
| `POST` | `/api/v1/accounts/drivers` | Public | Register a driver |
| `POST` | `/api/v1/auth/login` | Public | Obtain a 60 minute JWT |
| `GET` | `/api/v1/accounts/me` | Any valid JWT | View own profile |
| `PATCH` | `/api/v1/accounts/me` | Any valid JWT | Update own name and phone |
| `PATCH` | `/api/v1/accounts/{accountId}/status` | ADMIN JWT | Suspend or reactivate an account |
| `GET` | `/internal/v1/accounts/{accountId}/summary` | Service token | Return role and status for another service |

Swagger UI: `http://localhost:8081/swagger-ui.html`<br>
OpenAPI JSON: `http://localhost:8081/v3/api-docs`<br>
Health: `http://localhost:8081/actuator/health`

## Required environment variables

```text
ACCOUNT_DB_URL       JDBC URL for this service's own PostgreSQL database
ACCOUNT_DB_USERNAME  database username
ACCOUNT_DB_PASSWORD  database password
JWT_SECRET           random HS256 secret containing at least 32 characters
SERVICE_TOKEN        random internal token containing at least 32 characters
```

The four `BOOTSTRAP_ADMIN_*` variables are optional. Set all four on the first run to create the initial admin account. A later restart does not replace its password or details.

```text
BOOTSTRAP_ADMIN_EMAIL
BOOTSTRAP_ADMIN_PASSWORD
BOOTSTRAP_ADMIN_FULL_NAME
BOOTSTRAP_ADMIN_PHONE
```

Never commit real values. The root `.env.example` contains harmless placeholders.

## Quick local setup with Docker

From this directory:

```powershell
$env:ACCOUNT_DB_PASSWORD = "choose-a-local-password"
docker compose up -d

$env:ACCOUNT_DB_URL = "jdbc:postgresql://localhost:55431/account_db"
$env:ACCOUNT_DB_USERNAME = "postgres"
$env:JWT_SECRET = "replace-with-a-random-jwt-secret-32-chars-minimum"
$env:SERVICE_TOKEN = "replace-with-a-random-service-token-32-chars-minimum"
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
$env:BOOTSTRAP_ADMIN_PASSWORD = "AdminPass1"
$env:BOOTSTRAP_ADMIN_FULL_NAME = "RideLink Administrator"
$env:BOOTSTRAP_ADMIN_PHONE = "+94770000000"
.\mvnw.cmd spring-boot:run
```

Flyway creates the tables automatically. Docker stores the database in the named `account-db-data` volume.

To stop the local database:

```powershell
docker compose down
```

## Run the tests

```powershell
.\mvnw.cmd clean test
```

The H2 test profile runs the real Flyway migration. Integration tests cover registration, role injection, duplicate email, password policy, BCrypt storage, login, JWT claims and expiry, profiles, admin access, suspension, audit history and internal token protection.

## Demonstration order

1. Start the Account Service with the optional admin bootstrap variables.
2. Register a passenger and show the returned `PASSENGER` role.
3. Register a driver and show the returned `DRIVER` role.
4. Log in as each account and let Postman save their JWTs.
5. View and update a signed in account's own profile.
6. Log in as the admin and suspend the passenger.
7. Show that the suspended passenger can no longer log in.
8. Call the internal summary with the service token and show that no personal data is returned.
9. Repeat an internal call with the wrong service token to show `401 INVALID_SERVICE_TOKEN`.

Import `postman/RideLink-Account-Service.postman_collection.json` with the local environment to run this flow.
