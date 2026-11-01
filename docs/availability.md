# Availability, live updates and staff workspaces

This guide covers the backend features that complete the target architecture: geographic search with PostGIS, the Redis cache, live updates across instances, and the endpoints behind the staff and Ministry screens.

## Local topology

`docker compose up -d --build` starts:

| Service | Role |
| --- | --- |
| `postgres` | PostgreSQL 17 with PostGIS (`imresamu/postgis`, multi-architecture) |
| `redis` | Shared availability cache and Pub/Sub channel |
| `backend-1`, `backend-2` | Two identical, stateless Spring Boot instances |
| `gateway` | Nginx load balancer on `localhost:8080` (round robin, passive health checks, SSE-friendly) |
| `frontend` | React PWA on `localhost:5173`; its Nginx forwards `/api` to the gateway |

Existing local databases are upgraded by migration V3, which enables PostGIS and adds hospital coordinates. Back up the volume first if it contains data you need.

## Public endpoints

| Method and path | Purpose |
| --- | --- |
| GET `/api/availability/nearby?type=MRI&lat=33.99&lng=-6.85&radiusKm=50&limit=20` | Hospitals within the radius with at least one available machine of the type, nearest first |
| GET `/api/availability/stream` | Server-Sent Events: `connected`, then `equipment-status` after every committed change |
| GET `/api/imaging-types` | Searchable imaging services |

The citizen position is rounded to two decimals (about 1 km) before it is used or cached, and it is never stored. The gateway logs paths without query strings.

Hospitals appear in nearby searches only after an administrator records their location with PATCH `/api/organization/hospitals/{id}/location` (`{"latitude": 34.03, "longitude": -5.0}`), or creates them with coordinates.

## How a change reaches every open map

1. A staff request reaches one instance through the gateway, for example `backend-2`.
2. The service commits the equipment change and its audit record in one transaction.
3. After the commit, the availability cache is cleared (shared in Redis, so for every instance).
4. The change is published on the Redis channel `healthlink:equipment-status`.
5. Every instance, including `backend-1`, receives it and writes it to its own open SSE connections.

Nothing is announced when a transaction rolls back. Redis Pub/Sub keeps no history, so a browser that reconnects reloads the current state through REST. If Redis is unavailable, changes are still saved; cache entries expire after `HEALTHLINK_CACHE_TTL` (60 s by default).

Try it: open `http://localhost:8080/api/availability/stream` in a browser tab, then report a fault in Swagger. The event shows `publishedBy`, the instance that committed the change, which may differ from the instance serving your stream.

Cached values are stored as JSON text, not serialized Java objects, so a tampered cache entry cannot trigger Java deserialization attacks.

## Staff workspace endpoints

| Method and path | Who | Purpose |
| --- | --- | --- |
| GET `/api/equipment?departmentId=&status=` | Hospital staff | Inventory of your hospital; operators see only their department |
| GET `/api/work-orders/queue?status=` | Hospital staff | Unverified repairs of your hospital, oldest first |
| GET `/api/staff?hospitalId=` | PLATFORM_ADMIN, HOSPITAL_MANAGER | Staff profiles; managers see only their hospital |
| POST `/api/staff/{id}/deactivate`, `/activate` | PLATFORM_ADMIN, HOSPITAL_MANAGER | Soft deactivation; managers manage operators and technicians of their hospital; nobody changes their own access |
| POST `/api/imaging-types` | PLATFORM_ADMIN | Add an imaging service as data |
| GET `/api/ministry/availability?imagingType=` | MINISTRY_OFFICER | Machines per status by region, plus national totals |

All list endpoints use the existing page format: zero-based `page`, `size` from 1 to 100.

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| `HEALTHLINK_REDIS_ENABLED` | `false` (Compose: `true`) | Use Redis for the cache and Pub/Sub; when false, a single instance uses memory |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | `localhost`, `6379`, empty | Redis connection |
| `HEALTHLINK_CACHE_ENABLED` | `true` | Disable to always read from the database |
| `HEALTHLINK_CACHE_TTL` | `PT60S` | Maximum age of a cached search |
| `HEALTHLINK_LIVE_MAX_CONNECTIONS` | `1000` | Open SSE connections allowed per instance |

## Verification

Backend tests cover scoping and permissions of every new endpoint, cache clearing after commits, and SSE delivery without staff data or notes. The PostgreSQL test suite (CI) checks the V3 migration and runs the PostGIS query against real distances between Rabat, Fes and Casablanca.
