# Backend FUT — I.E. Alfonso Ugarte

REST backend built with Java 25, Spring Boot, Maven, JPA/Hibernate, and SQL Server. The package organization follows the reference project pattern provided by the instructor: `model`, `repository`, `service`, `service/impl`, `rest`, and `dto`.

## Before Running

1. Use JDK 25 (`java -version`) and Maven, or the included wrapper (`mvnw.cmd` on Windows / `./mvnw` on Linux or macOS).
2. Start SQL Server using the original compose file for your environment. The Docker files for Jimena and Viviana are preserved as-is.
3. For an existing database, review and run `database/migracion_compatibilidad_frontend.sql` on `AlfonsoUgarteDB` before starting the backend. The migration is additive: it adds missing columns and widens the document column to support DNI/CNE; it does not delete tables or records.
4. For a fresh installation, use `src/main/resources/sql/db.sql` and review the script with the database administrator first.
5. The application uses `ddl-auto: none`; it does not automatically create or modify the production schema.

## Build and Run

Windows:
```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

Linux/macOS:
```bash
./mvnw clean package
./mvnw spring-boot:run
```

The project requires Java 25. The application listens on port `8085`. The original SQL Server configuration is stored in `src/main/resources/application.yml` and in the original Docker files; it can also be configured via `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. Do not publish credentials in documentation or repositories.

When running the backend directly from Windows, the default configuration uses
`localhost:1433`, which corresponds to `docker-compose-jimena/docker-compose-db.yml`.
If you use the Viviana configuration, set the following before starting:

```powershell
$env:DATABASE_URL = "jdbc:sqlserver://localhost:1434;databaseName=AlfonsoUgarteDB;encrypt=true;trustServerCertificate=true"
```

## FUT Endpoints Consumed by Angular

- `POST /v1/api/solicitud` — register FUT request.
- `GET /v1/api/solicitud/seguimiento/{codigo}` — query status by tracking code.
- `GET /v1/api/solicitud` — list requests; supports `?estado=RECIBIDO`.
- `GET /v1/api/solicitud/eliminadas` — list logically deleted requests.
- `GET /v1/api/solicitud/{id}` — detail and history.
- `PUT /v1/api/solicitud/{id}` — update data.
- `DELETE /v1/api/solicitud/{id}` — logical deletion.
- `PATCH /v1/api/solicitud/{id}/restaurar` — restore request.
- `PATCH /v1/api/solicitud/{id}/estado` — change status and record history. JSON: `{"estado":"EN_REVISION","observacion":"Under review"}`.
- `GET /v1/api/tipo-tramite` — procedure type catalog.

Swagger UI: `http://localhost:8085/swagger-ui.html`. OpenAPI JSON: `http://localhost:8085/api-docs`.

## Email and Attachments

The backend stores and returns the applicant's email address, but does not send automatic emails on registration or status change. The frontend saves the attached file in its local storage; the backend stores the metadata sent by the form, not the binary file content.

## Tests

The context test uses a temporary H2 in-memory database under the `test` profile to verify the Spring context loads without requiring a SQL Server instance. Tests against the real database and endpoints must be run in an environment with JDK 25, SQL Server, and the corresponding configuration. The SQL Server connection should not be considered verified until tested in that environment.

See `INFORME_REVISION_Y_PRUEBAS.md` for the change summary and pending verifications.



