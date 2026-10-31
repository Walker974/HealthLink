# Maintenance workflow and audit history

The maintenance module implements the repair cycle in the architecture: an operator reports a fault, a technician accepts and completes the repair, and a different manager verifies it before equipment returns to public availability. It follows the current `ma.aui.csc.monolith.healthlink` package structure: each module has `controller`, `service` and `data` packages. DTOs and MapStruct mappers live directly in `data`, with entities and repositories in its subpackages.

## State transitions

| Action | Required actor | Work order after the action | Equipment after the action |
| --- | --- | --- | --- |
| Report fault | Operator in the equipment's department | OPEN | OUT_OF_SERVICE |
| Accept repair | Technician in the equipment's hospital | IN_PROGRESS | UNDER_MAINTENANCE |
| Complete repair | Assigned technician | AWAITING_VERIFICATION | UNDER_MAINTENANCE |
| Verify repair | Different manager in the same hospital | VERIFIED | AVAILABLE |
| Reject repair | Different manager in the same hospital | IN_PROGRESS | UNDER_MAINTENANCE |

After rejection, the assigned technician submits another completion. Only successful verification releases the equipment's open-work-order slot. There can be multiple historical verified orders, but only one unverified order per machine. Actor identities are resolved from the authenticated Keycloak subject and the active local staff assignment. Request bodies cannot choose who reported, repaired or verified a fault.

Self-verification and self-rejection are checked against both local staff ID and Keycloak subject. Changing a repairer's role to manager does not let that person review their own work.

## API

All endpoints below require a Keycloak bearer token. Existing service checks also enforce hospital and department access. A platform administrator has no equipment or maintenance access by default.

| Method and path | Request fields | Result |
| --- | --- | --- |
| GET `/api/equipment/{id}` | Equipment ID | Current equipment details and version |
| POST `/api/work-orders` | `equipmentId`, `equipmentVersion`, `description` | 201 and the new work order |
| GET `/api/work-orders/{id}` | Work-order ID | Current work order and version |
| GET `/api/work-orders?equipmentId={id}` | Optional `page`, `size` | Scoped, newest-first work-order history |
| POST `/api/work-orders/{id}/accept` | `version` | Assigned repair in progress |
| POST `/api/work-orders/{id}/complete` | `version`, `repairNotes` | Repair awaiting verification |
| POST `/api/work-orders/{id}/verify` | `version`, `notes` | Verified repair and restored availability |
| POST `/api/work-orders/{id}/reject` | `version`, `notes` | Repair returned to the same technician |
| GET `/api/equipment/{id}/history` | Optional `page`, `size` | Scoped, oldest-first append-only audit history |

Pages are zero-based and sizes are limited to 1–100. Descriptions, repair notes and review notes are required and limited to 2,000 characters. Use technical equipment information only; patient information is outside the project scope.

The old PATCH `/api/equipment/{id}/status` route is deprecated. It now requires `status: OUT_OF_SERVICE`, the current equipment `version`, and a fault `description`; it delegates to the work-order service and creates the same audit history. It cannot restore availability. Duplicate fault reports return 409 rather than silently creating more repairs.

## Try the workflow in Swagger

Create separate Keycloak users with RADIOLOGY_OPERATOR, MAINTENANCE_TECHNICIAN and HOSPITAL_MANAGER roles, and link each to the appropriate local profile through the existing staff API. The operator also requires a department. See the [security setup guide](security.md).

1. Authorize as the operator and GET the equipment. Copy its `version` into `equipmentVersion` when reporting the fault.
2. Report the fault with POST `/api/work-orders`. Record the returned work-order `id` and `version`.
3. Authorize as the technician. Accept the order using its current `version`. Each successful action returns the next version; use that value for the following action.
4. Complete the repair with technical repair notes. Confirm the equipment is still absent from the public availability search.
5. Authorize as the manager and verify or reject the repair. Verification restores public availability; rejection keeps the equipment unavailable.
6. Read `/api/equipment/{id}/history` to inspect actors, state changes and the notes from each action.

Swagger exposes the Maintenance and Audit tags. JWTs expire after five minutes in the local realm, so obtain fresh tokens and update authorization when needed.

## Concurrency and transaction boundaries

Business entities inherit a JPA `@Version` field. Fault reporting requires the version the client read from the equipment; repair actions require the version read from the work order. Stale requests and invalid state transitions return 409. JPA also detects competing updates that race after the initial version check.

A nullable, unique `open_equipment_id` reserves a machine while its work order is unverified. PostgreSQL additionally checks that this slot agrees with the order's status and equipment ID. This protects against concurrent duplicate reports even if an application-level check races.

Each action updates the equipment/work order and inserts its audit record in one transaction. Failure to write audit history rolls back the business change. JPA creation and modification metadata resolves the current staff member through the security context; the repository lookup suppresses automatic flush to avoid recursive auditing.

Status-change events contain immutable scalar snapshots. Delivery runs through `@TransactionalEventListener(AFTER_COMMIT)`, never before commit. Rollbacks discard pending notifications. A notifier failure is logged after the database commit rather than returning a misleading transaction failure. Delivery is currently best effort through the logging notifier; Redis/SSE and a durable transactional outbox remain later work.

AuditRecord does not extend BaseEntity. Its Java mapping is immutable, its repository exposes only append/read methods, and PostgreSQL triggers reject UPDATE, DELETE and TRUNCATE. Identifiers and actor identity are snapshots without cascading foreign keys, so resource deactivation or deletion does not erase history. Deployment should still use a separate migration owner and an application database role with SELECT/INSERT-only audit privileges; a database owner can alter the guards.

## Database migrations

Flyway now owns the schema; Hibernate uses `ddl-auto=validate`.

- V1 creates the original schema for new installations, preserving the existing Long-ID and sequence conventions.
- V2 adds version columns, work orders, audit records, constraints and append-only triggers.
- An existing original database is baselined at version 1, then receives V2 without dropping its existing tables, records or sequences.

The local Compose backend sets `FLYWAY_BASELINE_ON_MIGRATE=true` by default to adopt the project's original Hibernate-managed database. Main application configuration defaults this to false. Back up an existing database before its first upgrade; enable baselining only after confirming it is the expected original schema, then set the value to false after adoption. An unexpected or partially upgraded schema should be investigated rather than reset or automatically cleaned. Flyway clean is disabled.

For host execution, set the flag explicitly for that first legacy adoption:

```powershell
$env:FLYWAY_BASELINE_ON_MIGRATE = 'true'
.\backend\gradlew.bat -p backend bootRun
```

H2-backed tests use Hibernate-generated disposable schemas and disable Flyway. Opt-in PostgreSQL tests verify fresh migrations, preservation of legacy records, production Hibernate validation, the unique open-order constraint and audit mutation guards. They require a disposable database named `healthlink_maintenance_test_*` and these environment variables:

```text
HEALTHLINK_POSTGRES_TEST_URL=jdbc:postgresql://localhost:15432/healthlink_maintenance_test_<unique-suffix>
HEALTHLINK_POSTGRES_TEST_USER=<test-database-user>
HEALTHLINK_POSTGRES_TEST_PASSWORD=<test-database-password>
```

Run `.\backend\gradlew.bat -p backend test bootJar` from the project directory. PostgreSQL tests are skipped unless the test URL is supplied, and reject database names outside the disposable prefix.

## Remaining scope

Work-order cancellation/reassignment, full staff management, geographic searches, Redis delivery, SSE, the React PWA and ministry summaries are separate milestones. Existing legacy faulty equipment can enter this workflow by receiving a new fault report with its current version. Existing imaging-type provisioning remains unchanged. Business entities continue to use Long identifiers; audit entries use UUID identifiers.
