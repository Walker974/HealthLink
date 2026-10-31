# HealthLink backend

The existing modular Spring Boot application now lives in this folder. Java packages, database identifiers, migrations and authorization boundaries are preserved.

From this folder, after providing the database environment variables:

```powershell
.\gradlew.bat bootRun
.\gradlew.bat test bootJar
```

From the repository root, Docker Compose starts the backend and its dependencies:

```powershell
docker compose up -d --build backend
```

`exportOpenApi` exports the tested API contract to the sibling frontend using disposable H2 test infrastructure:

```powershell
.\gradlew.bat exportOpenApi
```

Shared local credentials and Keycloak configuration remain at the repository root. See [security](../docs/security.md), [maintenance](../docs/maintenance.md) and [frontend integration](../docs/frontend.md).
