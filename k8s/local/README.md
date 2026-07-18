# Local kind cluster — full Istio stack on your laptop

Run the entire platform (5 services + Istio mTLS + zero-trust authorization + in-cluster Postgres/Kafka/Redis) on a local [kind](https://kind.sigs.k8s.io/) cluster.

## Prerequisites

| Tool | Install |
|---|---|
| Docker Desktop | https://www.docker.com/products/docker-desktop/ |
| kind | `choco install kind` / `brew install kind` |
| kubectl | `choco install kubernetes-cli` / `brew install kubectl` |
| istioctl | https://istio.io/latest/docs/setup/getting-started/#download |
| JDK 17+, Maven | already on this machine |

## One command

```bash
bash scripts/kind-up.sh      # build, load images, install Istio, deploy everything
# ... work with it ...
bash scripts/kind-down.sh    # delete the cluster
```

> On Windows, run these from **Git Bash** (Docker Desktop must be running).

## What the script does

1. Creates a 3-node kind cluster, mapping ingress NodePorts 30080/30443 → `localhost:80/443`.
2. Installs Istio with a NodePort ingress gateway ([`istio-kind-install.yaml`](istio-kind-install.yaml)).
3. Builds the service jars and Docker images, then `kind load`s them (no registry needed).
4. Deploys in-cluster infra ([`infra.yaml`](infra.yaml)) — Postgres, Kafka, Redis (sidecar-excluded).
5. Applies ServiceAccounts + RBAC + local config, then the 5 workloads.
6. Applies the local mesh ([`istio-local.yaml`](istio-local.yaml)): STRICT mTLS, default-deny + identity-based `AuthorizationPolicy`, and HTTP routing.

## Files (local-only)

| File | Purpose |
|---|---|
| `kind-cluster.yaml` | kind cluster topology + port mappings |
| `istio-kind-install.yaml` | IstioOperator (NodePort ingress, small sidecars) |
| `infra.yaml` | Postgres/Kafka/Redis for local dev |
| `config-local.yaml` | ConfigMap/Secret pointing at in-cluster infra |
| `istio-local.yaml` | HTTP gateway + STRICT mTLS + zero-trust authz |

## How local differs from production (`k8s/istio/`)

| Aspect | Local (`k8s/local/`) | Production (`k8s/istio/`) |
|---|---|---|
| Gateway | HTTP (no cert) | HTTPS with TLS secret |
| End-user JWT at mesh | validated at app layer (HS256) | `RequestAuthentication` + JWKS (RS256) |
| Authorization | mTLS identity (default-deny + allow) | identity **+** JWT role claims |
| Canary subsets | omitted (single version) | `v1`/`v2` `DestinationRule` subsets |
| Datastores | in-cluster pods | RDS / MSK / ElastiCache |

## Prove the mesh is working (great for interview screenshots)

```bash
# 1. mTLS certificates issued to the sidecar
istioctl proxy-config secret deploy/api-gateway -n connected-cars

# 2. Zero-trust: default-deny + policies
kubectl get peerauthentication,authorizationpolicy -n connected-cars

# 3. Visual topology with mTLS lock icons
istioctl dashboard kiali

# 4. Negative test — a pod without the api-gateway identity is denied.
kubectl run rogue --image=curlimages/curl -n connected-cars --restart=Never -it --rm -- \
  curl -s -o /dev/null -w "%{http_code}\n" http://fleet-management-service:8084/api/v1/fleet/vehicles
# expect: 403  (RBAC: access denied — not the api-gateway principal)
```
