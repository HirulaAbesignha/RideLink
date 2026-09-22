# driver-vehicle-service

Owner: replace with the assigned member name and student ID.

- Port: `8082`
- Database: `driver_db`
- Swagger: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

## Run tests

```powershell
.\mvnw.cmd test
```

## Run locally

Create `driver_db` in PostgreSQL, then define the database password for this terminal:

```powershell
$env:DRIVER_DB_PASSWORD = "your-local-password"
.\mvnw.cmd spring-boot:run
```

Do not commit passwords. Add business code through a feature branch and pull request.
