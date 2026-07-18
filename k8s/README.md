# Kubernetes & Istio Manifests

Apply order matters. Use [`scripts/deploy-k8s.sh`](../scripts/deploy-k8s.sh) or run manually:

```bash
# 0. Prereqs: an EKS/kind cluster + istioctl installed
istioctl install --set profile=demo -y

# 1. Namespace (enables sidecar injection)
kubectl apply -f base/namespace.yaml

# 2. Identity, RBAC, and network firewall
kubectl apply -f base/serviceaccounts.yaml
kubectl apply -f base/rbac.yaml
kubectl apply -f base/network-policy.yaml

# 3. Config + secrets (EDIT config.yaml first — placeholders only)
kubectl apply -f deployments/config.yaml

# 4. Workloads (Deployments + Services + HPA)
kubectl apply -f deployments/

# 5. Istio security + traffic (order within this dir is not significant)
kubectl apply -f istio/

# 6. Observability (needs Prometheus Operator CRDs)
kubectl apply -f observability/
```

## Directory map

| Path | Contents |
|---|---|
| `base/` | Namespace, ServiceAccounts, RBAC (`Role`/`RoleBinding`), NetworkPolicy |
| `deployments/` | ConfigMap+Secret, per-service Deployment+Service, HPA + PDB |
| `istio/` | `PeerAuthentication` (mTLS), `DestinationRule`, `Gateway`, `VirtualService`, `RequestAuthentication` (JWT), `AuthorizationPolicy` |
| `observability/` | Istio `Telemetry`, Prometheus `ServiceMonitor` |

## What each Istio file does

| File | Kind | Purpose |
|---|---|---|
| `peer-authentication.yaml` | `PeerAuthentication` | STRICT mTLS mesh-wide + namespace |
| `destination-rule.yaml` | `DestinationRule` | Client mTLS, connection pools, circuit breaking, subsets |
| `gateway.yaml` | `Gateway` | Edge TLS termination for `api.connected-cars.toyota.com` |
| `virtual-service.yaml` | `VirtualService` | Routing, retries, timeouts, canary split |
| `request-authentication.yaml` | `RequestAuthentication` | Validate end-user JWT (issuer/audience/JWKS) |
| `authorization-policy.yaml` | `AuthorizationPolicy` | Default-deny + per-service allow + role-based (JWT claim) rules |

Deep explanation with descriptions: [`docs/interview-prep/istio-service-mesh-guide.md`](../docs/interview-prep/istio-service-mesh-guide.md) and [`docs/interview-prep/kubernetes-authorization-guide.md`](../docs/interview-prep/kubernetes-authorization-guide.md).

## Verify

```bash
# mTLS status
istioctl authn tls-check api-gateway.connected-cars.svc.cluster.local

# Authorization policies loaded
kubectl get authorizationpolicy,peerauthentication,requestauthentication -n connected-cars

# RBAC checks
kubectl auth can-i get secrets --as=system:serviceaccount:connected-cars:vehicle-telemetry-service -n connected-cars
```
