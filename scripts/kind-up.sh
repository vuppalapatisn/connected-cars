#!/usr/bin/env bash
# Spin up the FULL Istio stack on a local kind cluster.
# Prereqs: docker, kind, kubectl, istioctl, java 17+, maven.
#   bash scripts/kind-up.sh
set -euo pipefail

CLUSTER=connected-cars
REG=ghcr.io/vuppalapatisn/connected-cars
SERVICES=(api-gateway auth-service vehicle-telemetry-service notification-service fleet-management-service)
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> [1/8] Create kind cluster"
if ! kind get clusters | grep -q "^${CLUSTER}$"; then
  kind create cluster --config k8s/local/kind-cluster.yaml
fi
kubectl config use-context "kind-${CLUSTER}"

echo "==> [2/8] Install Istio (NodePort ingress for kind)"
istioctl install -f k8s/local/istio-kind-install.yaml -y

echo "==> [3/8] Namespace + sidecar injection"
kubectl apply -f k8s/base/namespace.yaml

echo "==> [4/8] Build service jars"
mvn -q clean package -DskipTests

echo "==> [5/8] Build + load images into kind"
for svc in "${SERVICES[@]}"; do
  docker build -q -f "services/${svc}/Dockerfile" -t "${REG}/${svc}:1.0.0" . >/dev/null
  kind load docker-image "${REG}/${svc}:1.0.0" --name "${CLUSTER}"
  echo "    loaded ${svc}"
done

echo "==> [6/8] Deploy in-cluster infra (postgres, kafka, redis)"
kubectl apply -f k8s/local/infra.yaml
kubectl -n connected-cars rollout status deploy/postgres --timeout=180s

echo "==> [7/8] Deploy identity, config, and workloads"
kubectl apply -f k8s/base/serviceaccounts.yaml
kubectl apply -f k8s/base/rbac.yaml
kubectl apply -f k8s/local/config-local.yaml
for svc in "${SERVICES[@]}"; do
  kubectl apply -f "k8s/deployments/${svc}.yaml"
done

echo "==> [8/8] Apply local Istio mesh (mTLS + zero-trust authz + routing)"
kubectl apply -f k8s/local/istio-local.yaml

echo "==> Waiting for the gateway..."
kubectl -n connected-cars rollout status deploy/api-gateway --timeout=240s

cat <<'EOF'

===================================================================
  Stack is up. Try it (ingress is mapped to http://localhost):

  curl -s http://localhost/actuator/health

  TOKEN=$(curl -s -X POST http://localhost/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"fleetadmin","password":"password"}' \
    | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

  curl -s -X POST http://localhost/api/v1/fleet/vehicles \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d '{"vin":"JT001","model":"bZ4X","modelYear":2025,"owner":"acme"}'

  Verify the mesh:
    istioctl proxy-config secret deploy/api-gateway -n connected-cars   # mTLS certs
    kubectl get peerauthentication,authorizationpolicy -n connected-cars
    istioctl dashboard kiali    # visualize mTLS + traffic

  Tear down:  bash scripts/kind-down.sh
===================================================================
EOF
