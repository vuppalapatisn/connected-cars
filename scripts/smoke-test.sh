#!/usr/bin/env bash
# End-to-end smoke test against the local docker-compose stack (gateway on :8080).
set -euo pipefail

GW=${GATEWAY_URL:-http://localhost:8080}

echo "==> Health"
curl -fsS "$GW/actuator/health" && echo

echo "==> Login as fleetadmin"
TOKEN=$(curl -fsS -X POST "$GW/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"fleetadmin","password":"password"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
echo "token: ${TOKEN:0:24}..."

echo "==> Register a vehicle (requires ROLE_FLEET_ADMIN)"
curl -fsS -X POST "$GW/api/v1/fleet/vehicles" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"vin":"JT2025BZ4X001","model":"bZ4X","modelYear":2025,"owner":"acme-fleet"}' && echo

echo "==> Ingest telemetry"
curl -fsS -X POST "$GW/api/v1/telemetry" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"vin":"JT2025BZ4X001","latitude":35.68,"longitude":139.69,"speedKph":42,"batteryPercent":9,"fuelPercent":0,"diagnosticCode":""}' && echo

echo "==> Fetch latest telemetry"
curl -fsS "$GW/api/v1/telemetry/JT2025BZ4X001/latest?limit=5" \
  -H "Authorization: Bearer $TOKEN" && echo

echo "==> Alerts (low battery should have fired via Kafka)"
sleep 2
curl -fsS "$GW/api/v1/notifications/alerts?limit=10" \
  -H "Authorization: Bearer $TOKEN" && echo

echo "==> Smoke test complete."
