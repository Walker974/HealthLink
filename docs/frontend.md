# Frontend architecture and development

HealthLink is one repository with independently built applications:

```text
HealthLink/
  backend/                  Spring Boot, Gradle wrapper, Java source and migrations
  frontend/                 React PWA, npm lockfile and frontend Dockerfile
  keycloak/                 Shared local realm configuration
  scripts/                  Local identity helpers
  docs/                     Architecture and implementation guides
  compose.yaml              Combined local stack
  .env                      Local Compose configuration (never browser configuration)
```

The backend's package structure and business rules are preserved. Existing database volumes and identifiers do not change because source files moved. Import `backend/build.gradle` when opening the Java project in an IDE. Old build output and local database backups were preserved under `backend/build/`.

## IntelliJ after moving the backend

Keep the repository root open, but link Gradle to `backend/build.gradle`. The repository root itself is no longer a Gradle build.

If synchronization says the `HealthLink` directory does not contain a Gradle build:

1. In the Gradle tool window, right-click the old root project and choose **Unlink Gradle Project**. This removes the IDE link; source files stay in place.
2. Choose **File → New → Module from Existing Sources**, then select `backend/build.gradle`.
3. Wait for Gradle synchronization to finish. Java source roots and generated MapStruct sources are imported from `backend/`.

Use the **Project Files** view to browse both applications from the repository root. Alternatively, open `backend/` separately in IntelliJ and `frontend/` in your frontend editor. Run backend Gradle commands from `backend/`, or use `-p backend` with the wrapper when your terminal is at the repository root.

## Frontend structure

The React application is feature-based:

```text
frontend/src/
  app/
    layouts/                Public and authenticated workspace shells
    providers/              Authentication, queries, PWA and workspace composition
    router.tsx              Route configuration and role boundaries
    styles.css              HealthLink visual language and responsive layouts
  components/
    ui/                     shadcn/ui source components
    shared/                 Brand, status badges, data table and page states
  features/
    auth/                   Keycloak adapter, staff session and sign-in
    availability/           Public search, hospital details, map and saved results
    equipment/              Equipment records and registration
    maintenance/            Fault reports, repair actions and audit history
    organization/           Regions, hospitals and departments
    staff/                  Profile and staff provisioning
    ministry/               Labelled reporting preview; live reporting is pending
  lib/
    api/                    HTTP boundary and generated OpenAPI contract types
    env.ts                  Public browser configuration
    query-client.ts         Server-state query policy
  test/                     Shared test setup
```

Each feature owns its API functions, types, components and pages. Add hooks beside the feature when extracting repeated behavior. Keep authorization, transactions and business invariants in Spring services. Frontend role checks guide the interface; the server remains authoritative. Shared UI components do not call APIs. Application providers compose features and keep explicit sample state separate from live requests.

The installed stack includes React, TypeScript, Vite, Tailwind CSS, shadcn/ui (Radix), Lucide, React Router, TanStack Query, TanStack Table, React Hook Form, Zod, Keycloak JS, React Leaflet/Leaflet, Recharts, Sonner, IndexedDB helpers and Vite PWA/Workbox. Vitest and Testing Library cover the HTTP boundary, offline snapshots and form conflicts. TypeScript is kept on the version supported by the OpenAPI generator; the npm lockfile records the complete dependency graph.

## Run the whole application

For real API data with sample mode disabled, follow the [live-mode setup and onboarding guide](live-mode.md).

Run from the repository root, with your existing `.env` and Docker Desktop's Linux engine:

```powershell
docker compose up -d --build
```

Open:

- Frontend: http://localhost:5173
- Explicit design preview: http://localhost:5173/?preview=1
- Backend Swagger: http://localhost:8080/swagger-ui/index.html
- Keycloak: http://localhost:8180/admin

The frontend image serves static files through an unprivileged Nginx container. `/api` is proxied to the backend, and client-side routes fall back to the application entry page. The manifest and service worker are refreshed without immutable HTTP caching; fingerprinted assets are cached. OIDC callback query strings are excluded from frontend access logging.

The root `.env` stays private to Compose and is outside the frontend build context. `VITE_*` configuration is public and embedded at build time. Never put database passwords, client secrets or bearer tokens in frontend environment files. Build arguments in Compose provide local Keycloak settings. Rebuild the frontend image after changing those values.

## Develop the frontend independently

Use Node.js 22.12+ and npm. If the frontend container already owns port 5173, stop that service first:

```powershell
docker compose stop frontend
docker compose up -d --build gateway
npm --prefix frontend ci
npm --prefix frontend run dev
```

Use **http://localhost:5173**, matching the existing Keycloak redirect URI. Vite binds to the loopback interface and proxies `/api` to port 8080. Optional local overrides can be placed in `frontend/.env.local`, using `frontend/.env.example` as the reference. Application source and styles update through Vite's development server. Service-worker registration is enabled in the production build, keeping development free of stale worker caches.

To check the frontend independently:

```powershell
npm --prefix frontend run test
npm --prefix frontend run build
npm --prefix frontend run preview
```

The preview server uses port 4173. Public pages and PWA behavior can be inspected there; Keycloak sign-in uses the registered port 5173 unless its redirect configuration is deliberately expanded. The production frontend in Compose uses port 5173 and is the normal integration target.

## API contract

The frontend tracks the real Spring OpenAPI document in `frontend/openapi/healthlink.json`. Refresh it without starting PostgreSQL or Keycloak:

```powershell
.\backend\gradlew.bat -p backend exportOpenApi
npm --prefix frontend run api:generate
```

The Gradle task runs the public contract test with disposable H2 infrastructure and exports the controller-generated schema. The npm command generates TypeScript contract types from that snapshot, with no network dependency. To deliberately refresh from a running API instead:

```powershell
npm --prefix frontend run api:generate -- http://localhost:8080/v3/api-docs
```

Regenerate the snapshot/types when changing response DTOs or requests. Request validation in the browser improves feedback; server validation still applies. The public equipment response now includes hospital ID, name, city and address, allowing public search to show hospital cards without calling protected department APIs.

## Connected screens and remaining API work

| Screen | Current behavior |
| --- | --- |
| Public availability | Live search by city and imaging type; machines grouped by hospital |
| Hospital detail | Public hospital record and per-type availability |
| Staff sign-in | Existing `healthlink-web` client, Authorization Code + PKCE S256 |
| Equipment | Scoped record lookup by ID and manager registration |
| Repairs | Work orders for the opened equipment; accept, complete, verify or reject |
| Audit history | Scoped, paginated equipment history |
| Departments | Manager listing and creation in the assigned hospital |
| Platform administration | Region/hospital creation, hospital directory, local staff linking |
| Ministry | Explicit sample dashboard; no live national statistics are fabricated |

The current backend has no hospital-wide equipment collection, global repair inbox, staff directory, ministry summary endpoint, hospital phone field, or verified hospital coordinates. The live UI uses record lookup and supported endpoints. Design previews show the intended richer experience while those APIs are developed. The public map displays illustrative fixture positions only in preview mode; live results use actual hospital names and addresses without invented distances or coordinates. Imaging codes must exist in the database; the initial choices follow the maquette's MRI, CT, X-ray and ultrasound categories.

Staff profiles are linked to existing Keycloak users. Provisioning in the UI does not create Keycloak accounts or assign realm roles. Operators require a department ID provided by their manager; platform administrators do not gain protected department/equipment reads.

## Design previews

The supplied HTML maquette informed the green/neutral palette, citizen search, hospital details, role workspaces and administration screens. The React version adds responsive navigation, keyboard-capable dialogs, explicit statuses, timestamps, loading/empty/error states, and feedback on conflicts.

Previews require both `VITE_ENABLE_DESIGN_PREVIEW=true` and `?preview=1`. Their banner identifies sample data. Switching a preview role never creates a token or grants API access. Sample changes stay in memory; failed live requests never silently switch to fixtures. The preview switcher is available from the sign-in page and within sample workspaces. Set `VITE_ENABLE_DESIGN_PREVIEW=false` when creating a deployment that should expose only live screens.

The maquette's optional fault description and undo shortcut were replaced with the backend's required description and verified repair lifecycle. Live status labels reflect fetch/status timestamps; Redis/SSE is still a separate backend milestone, so the interface does not claim a live event connection.

## Offline and installation

- The production build has a manifest, locally bundled fonts, PNG/vector icons, an application service worker and an opt-in update prompt.
- The worker precaches application assets, including lazy route chunks. It does not cache authenticated API responses, Keycloak requests or external map tiles.
- Successful public searches save a small IndexedDB snapshot keyed by city and imaging type. A failed connection can show that snapshot with its original timestamp and a stale-data notice. Explicit sample data is never saved as a live snapshot.
- Staff writes require a connection and are never queued offline. A 409 keeps form notes available and refreshes the affected records for review.
- Access/refresh tokens remain in the Keycloak adapter's memory. Staff query state is cleared on sign-out.
- Offline map tiles and geolocation-based distance sorting are not implemented. Tile-provider licensing and usage policies must be addressed before adding offline basemaps.

PWA installation varies by browser. The install control uses a native install prompt when provided, otherwise explains the browser's Add to Home Screen flow. Browser installation and device-specific behavior should be checked on the actual phones used for the demonstration.

## Dependencies and verification

Use `npm ci` for reproducible installation and keep the lockfile with dependency changes. Inspect production and toolchain audits separately:

```powershell
npm --prefix frontend audit --omit=dev
npm --prefix frontend audit
```

Compatible fixes were applied during setup. Some upstream development tools may still report advisories; do not blindly use `npm audit fix --force`, which can downgrade the PWA or code-generation stack across incompatible versions. Re-check advisories as dependencies release compatible updates. No claim of production hardening or measured Lighthouse targets is made by this scaffold.
