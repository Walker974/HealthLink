#!/usr/bin/env bash
# Loads LOCAL DEMO data into the Docker Compose stack:
#   - synthetic hospitals, equipment and repairs (scripts/demo/seed.sql)
#   - five demo Keycloak accounts linked to staff profiles (scripts/demo/keycloak-users.json)
# The accounts share one public demo password. Never run this against a shared or production deployment.
# Usage, from the repository root, after `docker compose up -d --build`:   bash scripts/demo/load-demo.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
set -a; . ./.env; set +a
KEYCLOAK="${KEYCLOAK_URL:-http://localhost:8180}"

echo "1/3 Waiting for the backend to finish its database migrations..."
ready=""
for attempt in $(seq 1 60); do
  if docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc \
      "SELECT 1 FROM information_schema.columns WHERE table_name = 'hospitals' AND column_name = 'latitude'" 2>/dev/null | grep -q 1; then
    ready="yes"; break
  fi
  sleep 3
done
if [ -z "$ready" ]; then
  echo "The database is not ready (migration V3 not applied). Check: docker compose logs backend-1" >&2
  exit 1
fi

echo "2/3 Loading demo hospitals, equipment and repairs..."
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -q -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  < scripts/demo/seed.sql > /dev/null

echo "3/3 Creating demo Keycloak accounts..."
token_response=$(curl -fsS -X POST "$KEYCLOAK/realms/master/protocol/openid-connect/token" \
  -d grant_type=password -d client_id=admin-cli \
  --data-urlencode "username=${KEYCLOAK_ADMIN_USERNAME:-admin}" \
  --data-urlencode "password=${KEYCLOAK_ADMIN_PASSWORD}")
token=$(printf '%s' "$token_response" | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
curl -fsS -X POST "$KEYCLOAK/admin/realms/healthlink/partialImport" \
  -H "Authorization: Bearer $token" -H "Content-Type: application/json" \
  --data @scripts/demo/keycloak-users.json > /dev/null

cat <<'DONE'

Demo data loaded. Open http://localhost:5173

  Citizen search : no account needed (try Rabat + MRI)
  Staff sign-in  : http://localhost:5173/staff  — password for every account: demo-2026

  manager     Hospital manager, Hay Riad (verifies repairs)
  operator    Radiology operator, Hay Riad (reports faults)
  technician  Maintenance technician, Hay Riad (accepts and completes repairs)
  ministry    Ministry officer (regional figures)
  admin       Platform administrator (regions, hospitals, staff)
DONE
