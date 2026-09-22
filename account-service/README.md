# account-service

Owner: replace with the assigned member name and student ID.

- Port: `8081`
- Database: `account_db`
- Swagger: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## Run tests

```powershell
.\mvnw.cmd test
```

## Run locally

Create `account_db` in PostgreSQL, then define the database password for this terminal:

```powershell
$env:ACCOUNT_DB_PASSWORD = "your-local-password"
.\mvnw.cmd spring-boot:run
```

Do not commit passwords. Add business code through a feature branch and pull request.
