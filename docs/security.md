# Spring Security and Keycloak

HealthLink is an OAuth 2.0 resource server. Staff sign in at Keycloak; the client sends the resulting **access token** as `Authorization: Bearer <token>`. The API verifies the RSA signature, issuer, audience and token lifetime using Spring Security. It does not accept passwords, HTTP Basic authentication or session cookies.

The implementation follows [Spring Security JWT resource server support](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) and [Spring Boot OAuth2 configuration](https://docs.spring.io/spring-boot/reference/security/oauth2.html). The local identity service uses the [Keycloak container and realm import mechanism](https://www.keycloak.org/server/containers).

## Start the local environment

1. Start Docker Desktop with the Linux container engine.
2. If `.env` already exists, add the missing variables from `.env.example`. For a new checkout, copy `.env.example` to `.env`. Replace all three password placeholders with local values.
3. Run:

   ```powershell
   docker compose config --quiet
   docker compose up -d --build
   docker compose ps
   ```

The API is at `http://localhost:8080`. The Keycloak admin console is at `http://localhost:8180/admin`, using `KEYCLOAK_ADMIN_USERNAME` and `KEYCLOAK_ADMIN_PASSWORD` from `.env`. Keycloak has a separate PostgreSQL database and persistent volume.

The `healthlink` realm is imported from `keycloak/healthlink-realm.json` on its first start. An existing realm is skipped on subsequent starts; editing the JSON does not update that existing realm. Apply changes through the console or an explicit realm migration. Do not delete database volumes to apply a configuration change.

This Compose configuration uses Keycloak `start-dev`, realm SSL requirement `none`, and loopback HTTP for local development. A deployed environment needs its own HTTPS hostname, realm SSL requirement `all`, production Keycloak configuration, database credentials, frontend redirects and CORS origins. Disable the `healthlink-cli` client when it is no longer needed.

## Roles and current endpoints

Only the five `StaffRole` names in `realm_access.roles` map to Spring authorities, with the `ROLE_` prefix. OAuth scopes, unrelated client roles and unknown realm roles do not grant application permissions. There is no role hierarchy: a platform administrator does not inherit hospital permissions.

| Endpoint | Permission |
| --- | --- |
| `GET /api/equipment/available?type=MRI&city=Fes` | Public citizen search |
| `GET /api/organization/regions` | Public reference data |
| `GET /api/organization/regions/{id}` | Public reference data |
| `GET /api/organization/regions/{id}/hospitals` | Public reference data |
| `GET /api/organization/hospitals/{id}` | Public reference data |
| `POST /api/organization/regions` | `PLATFORM_ADMIN` |
| `POST /api/organization/hospitals` | `PLATFORM_ADMIN` |
| `POST /api/staff` | `PLATFORM_ADMIN`; link an existing Keycloak account |
| `POST /api/organization/departments` | `HOSPITAL_MANAGER`, own hospital |
| `POST /api/equipment` | `HOSPITAL_MANAGER`, own hospital |
| `PATCH /api/equipment/{id}/status` | Deprecated fault report; `RADIOLOGY_OPERATOR`, own department; version and description required |
| `GET /api/equipment/{id}` and `GET /api/equipment/{id}/history` | Manager or technician in own hospital; operator in own department |
| `POST /api/work-orders` | `RADIOLOGY_OPERATOR`, own department |
| `GET /api/work-orders/{id}` and `GET /api/work-orders?equipmentId={id}` | Manager or technician in own hospital; operator in own department |
| `POST /api/work-orders/{id}/accept` | `MAINTENANCE_TECHNICIAN`, own hospital |
| `POST /api/work-orders/{id}/complete` | Assigned technician in own hospital |
| `POST /api/work-orders/{id}/verify` and `/reject` | `HOSPITAL_MANAGER`, own hospital; cannot review own repair |
| `GET /api/organization/departments/{id}` | Hospital manager, operator or technician, own hospital |
| `GET /api/organization/hospitals/{id}/departments` | Hospital manager, operator or technician, own hospital |
| `GET /api/staff/me` | Authenticated account with an active, matching local staff profile |
| All other routes | Denied until explicitly configured |

An unauthenticated protected request returns `401`. A valid token with insufficient permissions or an invalid local assignment returns `403`. A malformed, expired, incorrectly signed, wrong-issuer or wrong-audience token returns `401`, including when attached to a public request. Public callers should omit the header when they have no valid token.

Role checks also run on service methods. Hospital-specific operations resolve `StaffMember` by the JWT's immutable `sub` claim, then require an active profile with a matching role and hospital. Operators must also match the equipment's department. Hospital and department IDs in request bodies do not establish the caller's authority. Email is contact data, not an identity lookup key.

The initial platform administrator can onboard organizations and staff using its Keycloak role before it has a local staff profile. It still has no equipment access. Create its local profile if `/api/staff/me` is needed.

## Create users and link staff profiles

In the Keycloak admin console, switch from the `master` realm to **healthlink**. Create separate synthetic demonstration users and set their passwords under **Credentials**. Under **Role mapping**, assign the appropriate **realm role** from the five HealthLink roles. The bootstrap Keycloak administrator is not automatically a HealthLink application user.

Create a user with `PLATFORM_ADMIN` first. Then use the local device login helper to obtain an access token without sending its password to the API or a script:

```powershell
$adminToken = .\scripts\Get-HealthLinkToken.ps1
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
```

The helper prints a local Keycloak verification URL and a one-time code. Open that URL and sign in as the HealthLink platform administrator. The function returns the access token to the variable after login succeeds. Tokens expire after five minutes; run it again when needed. Do not commit or share tokens.

Use the administrator token to create a region and hospital through the organization endpoints. Create a separate Keycloak user with `HOSPITAL_MANAGER`, and copy its **user ID** from the console. Link it to the hospital:

```powershell
# Replace the user ID and hospital ID with values from your environment.
$managerProfile = @{
    keycloakSubject = '<manager Keycloak user ID>'
    fullName = 'Demo Manager'
    email = 'manager@example.test'
    role = 'HOSPITAL_MANAGER'
    hospitalId = 1
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/staff' `
    -Headers $adminHeaders -ContentType 'application/json' -Body $managerProfile
```

Sign in as that manager with the helper to create departments. The administrator can then link an operator with `role = 'RADIOLOGY_OPERATOR'`, the same `hospitalId`, and its `departmentId`. A technician or manager requires a hospital and no department; a ministry officer or platform administrator has neither. The server rejects assignments whose department belongs to another hospital. Keycloak role assignments and local profile roles must agree. Linking a profile does not grant a Keycloak role or create a Keycloak account.

Existing staff rows retain their original IDs and remain unlinked until migrated. An authorized database migration can populate `keycloak_subject` and an operator's `department_id` on those rows. Do not create duplicate profiles with the same email. The current provisioning endpoint creates profiles; profile updates and staff-management workflows remain future work.

Test the linked account:

```powershell
$staffToken = .\scripts\Get-HealthLinkToken.ps1
Invoke-RestMethod -Uri 'http://localhost:8080/api/staff/me' `
    -Headers @{ Authorization = "Bearer $staffToken" }

# Citizen search needs no token.
Invoke-RestMethod -Uri 'http://localhost:8080/api/equipment/available?type=MRI&city=Fes'
```

Equipment registration still requires an existing imaging type, as before this change. There is no imaging-type creation endpoint yet.

## Frontend integration

The imported `healthlink-web` client uses OpenID Connect Authorization Code flow with **PKCE S256**. It is public and needs no client secret. Password grants and implicit flow are disabled. Configure the future React client with:

| Setting | Local value |
| --- | --- |
| Keycloak base URL | `http://localhost:8180` |
| Realm | `healthlink` |
| Client ID | `healthlink-web` |
| Frontend origin | `http://localhost:5173` |
| API base URL | `http://localhost:8080` |

Attach the **access token**, not the ID token, to protected API requests. Refresh it through the identity client when needed. Keep tokens in memory. The realm's default client scope adds `aud: healthlink-api` and `realm_access.roles` to access tokens. No role is assigned automatically to a new user.

If the frontend origin changes, update both the Keycloak client's redirect URIs/web origins and `CORS_ALLOWED_ORIGINS`. Comma-separated allowed origins are supported. CORS preflight runs before authentication; cookie credentials are not enabled.

## Run the backend on the host

```powershell
docker compose up -d postgres keycloak
# Export the database password from your local configuration in this shell.
$env:DB_PASSWORD = '<local PostgreSQL password>'
.\backend\gradlew.bat -p backend bootRun
```

Spring does not automatically load Compose's `.env` file. When running on the host, supply `DB_PASSWORD` yourself; optional database URL and username overrides are `DB_URL` and `DB_USERNAME`.

The default issuer and JWK URL work on the host. Inside Compose, the issuer stays `http://localhost:8180/realms/healthlink` so it matches browser-issued tokens, while the JWK URL uses the internal address `http://keycloak:8080/.../certs`. Changing the issuer to the internal hostname would incorrectly reject browser tokens. Providing the JWK URL explicitly lets the backend start without identity-server discovery; protected requests still need valid signing keys.

Override `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_JWK_SET_URI` and `KEYCLOAK_AUDIENCE` for other deployments. The API uses Spring Boot's JWT validation settings rather than a custom authentication filter.

## Verification and remaining work

```powershell
.\backend\gradlew.bat -p backend test
docker compose config --quiet
```

Security tests use H2 and a local HTTP JWK server with real RSA-signed JWTs. They exercise signature, issuer, audience and expiry validation, public access, denied roles, profile provisioning, inactive profiles, hospital and department isolation, CORS and default denial. Maintenance tests additionally exercise independent verification, concurrent updates, atomic audit persistence and notifications after commit. These tests do not require a running Keycloak or PostgreSQL. Optional PostgreSQL migration and database-constraint tests are documented in the [maintenance guide](maintenance.md).

Work orders and append-only audit history now cover reporting, repair and independent verification. JPA creation and modification metadata resolves the current active staff profile from the JWT subject. Equipment becomes publicly available only after manager verification. See the [maintenance guide](maintenance.md) for endpoints, version checks and database migrations. The [React frontend](frontend.md) uses the existing public Keycloak client with Authorization Code and PKCE. Live ministry reporting, full staff management and Redis/SSE delivery remain future work.
