#!/usr/bin/env bash
# Deploy the platform to a Kubernetes cluster with Istio installed.
set -euo pipefail

echo "==> Installing Istio (demo profile) if not present"
if ! kubectl get ns istio-system >/dev/null 2>&1; then
  istioctl install --set profile=demo -y
fi

echo "==> Namespace + sidecar injection"
kubectl apply -f k8s/base/namespace.yaml

echo "==> Base: service accounts, RBAC, network policies"
kubectl apply -f k8s/base/

echo "==> Config + secrets (replace placeholders before prod!)"
kubectl apply -f k8s/deployments/config.yaml

echo "==> Workloads"
kubectl apply -f k8s/deployments/

echo "==> Istio: mTLS, routing, authentication, authorization"
kubectl apply -f k8s/istio/

echo "==> Observability"
kubectl apply -f k8s/observability/ || echo "(skip: Prometheus Operator CRDs not installed)"

echo "==> Waiting for rollouts"
kubectl -n connected-cars rollout status deploy/api-gateway --timeout=180s

echo "==> Verifying mTLS + policies"
istioctl authn tls-check api-gateway.connected-cars.svc.cluster.local || true
kubectl get authorizationpolicy -n connected-cars

echo "==> Done."
