# fare-payment-service

Owner: replace with the assigned member name and student ID.

- Port: `8084`
- Database: `payment_db`
- Swagger: `http://localhost:8084/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8084/v3/api-docs`

## Run tests

```powershell
.\mvnw.cmd test
```

## Run locally

Create `payment_db` in PostgreSQL, then define the database password for this terminal:

```powershell
$env:PAYMENT_DB_PASSWORD = "your-local-password"
.\mvnw.cmd spring-boot:run
```

Do not commit passwords. Add business code through a feature branch and pull request.
