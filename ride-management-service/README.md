# ride-management-service

Owner: replace with the assigned member name and student ID.

- Port: `8083`
- Database: `ride_db`
- Swagger: `http://localhost:8083/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`

## Run tests

```powershell
.\mvnw.cmd test
```

## Run locally

Create `ride_db` in PostgreSQL, then define the database password for this terminal:

```powershell
$env:RIDE_DB_PASSWORD = "your-local-password"
.\mvnw.cmd spring-boot:run
```

Do not commit passwords. Add business code through a feature branch and pull request.
