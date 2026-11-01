# OpenAPI and Swagger UI

HealthLink generates an OpenAPI specification from the existing controllers, DTOs, validation annotations and documentation annotations. Swagger UI lets you inspect and call these endpoints from the browser.

The integration uses [springdoc-openapi](https://springdoc.org/getting-started.html) 3.1.1 for Spring Boot 4.1. It documents the implemented Organization, Equipment, Staff, Maintenance and Audit APIs.

## Open the documentation

With the backend running at its default port:

| Resource | URL |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| OpenAPI YAML | http://localhost:8080/v3/api-docs.yaml |

If you run the backend with Docker, rebuild it after pulling this change:

```powershell
docker compose up -d --build gateway
```

For a backend running through Gradle, restart `bootRun`. The documentation routes are public; protected application operations still require their existing roles and organizational assignments. Only application paths under `/api/**` are included in the generated specification.

## Try public operations

Open the Organization or Equipment tag, expand an operation, select **Try it out**, fill in its inputs and select **Execute**. Public region and hospital reads and the equipment availability search need no token. The availability search accepts an imaging type code such as `MRI` and a city such as `Fes`.

Examples use synthetic data. Replace example IDs and the staff subject with values from your environment. Equipment registration requires an existing department and imaging type.

## Authorize staff operations

Obtain a fresh access token through the existing Keycloak helper:

```powershell
$adminToken = .\scripts\Get-HealthLinkToken.ps1
```

Follow the printed verification URL and sign in as a HealthLink user. To copy only the returned token to the Windows clipboard:

```powershell
Set-Clipboard -Value $adminToken
```

In Swagger UI, select **Authorize**, paste the access token into **bearerAuth**, then select **Authorize** and **Close**. Paste the token without the `Bearer` prefix; Swagger adds that prefix to requests. Authorization is kept in the current page rather than persisted across refreshes.

The imported local realm issues five-minute access tokens. When a request returns `401`, obtain a fresh token and replace it in Swagger's authorization dialog. `403` indicates that the current account lacks the required role or a valid local staff assignment. Role names and hospital or department requirements appear in each protected operation's description.

The Keycloak admin-console account is separate from a HealthLink application user. A `PLATFORM_ADMIN` can create regions, hospitals and staff profiles, but does not automatically have hospital-manager or operator permissions. The `/api/staff/me` operation also requires a linked local staff profile. See the [security guide](security.md) for onboarding instructions.

## Current API behavior

- Creation operations document `201 Created`; organization, equipment and work-order creation also return a `Location` header. Authorized hospital staff can GET equipment by ID to read its current version.
- Paginated organization reads accept a zero-based `page` and a `size` between 1 and 100, defaulting to 0 and 20.
- The deprecated equipment status endpoint accepts only `OUT_OF_SERVICE` and requires the equipment `version` and fault `description`. It creates a work order through the same maintenance service.
- The Maintenance tag documents fault reporting, technician acceptance/completion, and independent manager verification/rejection. Submit the current version with each action; stale versions, duplicate open orders and invalid transitions return `409`.
- The Audit tag exposes scoped equipment history. The [maintenance guide](maintenance.md) includes the full Swagger walkthrough and transaction rules.
- Staff provisioning links an existing Keycloak subject to a local profile. It does not create Keycloak users or assign realm roles.
- Shared error descriptions cover invalid inputs (`400`), authentication (`401`), permission failures (`403`), missing resources (`404`) and uniqueness or constraint conflicts (`409`). Response body formats remain those of the existing API and error handlers.

The OpenAPI security declarations describe authentication for API clients. Spring Security and the service authorization checks enforce the actual permissions.

## Frontend contract types

The React application in `frontend/` uses TypeScript types generated from the Spring controller contract. From the repository root, refresh the tracked schema and generated types with:

```powershell
.\backend\gradlew.bat -p backend exportOpenApi
npm --prefix frontend run api:generate
```

The export runs against disposable H2 test infrastructure. See the [frontend guide](frontend.md) for the feature structure, API integration and development workflow.
