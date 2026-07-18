# Connected Cars Platform

A cloud-native, event-driven **connected-vehicle platform** (inspired by a Toyota Connected Cars program) built with **Spring Boot microservices**, secured and routed with **Istio service mesh**, deployed on **Amazon EKS**, and provisioned with **Terraform**.

This repository is also an **interview-preparation companion** for AWS solution-architecture interviews (see [`docs/interview-prep/`](docs/interview-prep/)). It demonstrates, end to end:

- Service-to-service communication and **mutual TLS (mTLS)** with Istio.
- **Authorization** at the mesh layer (`AuthorizationPolicy`, `RequestAuthentication`) and the cluster layer (Kubernetes **RBAC**, `ServiceAccount`, IRSA).
- A production-shaped AWS reference architecture (EKS, MSK/Kafka, RDS, ElastiCache, API Gateway, ALB, IAM).

> ⚠️ This is a reference/education codebase. Secrets in manifests are placeholders — use AWS Secrets Manager / External Secrets in real deployments.

---

## Architecture at a glance

```
                       ┌────────────────────────────────────────────────────┐
   Vehicles / Mobile   │                   Amazon EKS (VPC)                  │
   ────────────────►   │                                                    │
        (MQTT/HTTPS)    │   ┌────────────┐   Istio Ingress Gateway          │
                        │   │  AWS ALB   │──────────┬───────────────────────│
                        │   └────────────┘          │  (mTLS STRICT mesh)   │
                        │                            ▼                        │
                        │   ┌───────────────────────────────────────────┐   │
                        │   │  api-gateway (Spring Cloud Gateway)        │   │
                        │   └───┬───────────┬────────────┬──────────────┘   │
                        │       │           │            │                   │
                        │       ▼           ▼            ▼                   │
                        │  ┌─────────┐ ┌──────────┐ ┌──────────────┐        │
                        │  │  auth   │ │telemetry │ │ notification │        │
                        │  │ service │ │ service  │ │   service    │        │
                        │  └────┬────┘ └────┬─────┘ └──────┬───────┘        │
                        │       │           │              │                 │
                        │       ▼           ▼              ▼                 │
                        │  ┌─────────┐ ┌──────────┐ ┌──────────────┐        │
                        │  │  RDS    │ │ Amazon   │ │ ElastiCache  │        │
                        │  │ (Postgres)│  MSK/Kafka│ │  (Redis)    │        │
                        │  └─────────┘ └──────────┘ └──────────────┘        │
                        │            fleet-management-service                │
                        └────────────────────────────────────────────────────┘
```

Full diagram and rationale: [`ARCHITECTURE.md`](ARCHITECTURE.md).

---

## Microservices

| Service | Port | Responsibility |
|---|---|---|
| `api-gateway` | 8080 | Edge routing, JWT validation, rate limiting (Spring Cloud Gateway) |
| `auth-service` | 8081 | Issues/validates JWTs (OAuth2 resource+auth), user & device identity |
| `vehicle-telemetry-service` | 8082 | Ingests vehicle telemetry, publishes to Kafka, stores time-series |
| `notification-service` | 8083 | Consumes events, sends push/SMS/email alerts to drivers |
| `fleet-management-service` | 8084 | Fleet inventory, vehicle registration, aggregated dashboards |

Each service is an independent Maven module under [`services/`](services/), containers via a multi-stage `Dockerfile`, and ships with Kubernetes + Istio manifests under [`k8s/`](k8s/).

---

## Quick start (local)

```bash
# 1. Build all services
./mvnw clean package -DskipTests

# 2. Run the full stack locally (Postgres, Kafka, Redis + services)
docker-compose up --build

# 3. Smoke test
curl http://localhost:8080/actuator/health
```

### API documentation (Swagger UI)

Every service exposes interactive OpenAPI docs at `/swagger-ui.html`:

| Service | Swagger UI |
|---|---|
| API Gateway (aggregates all specs) | http://localhost:8080/swagger-ui.html |
| auth-service | http://localhost:8081/swagger-ui.html |
| vehicle-telemetry-service | http://localhost:8082/swagger-ui.html |
| notification-service | http://localhost:8083/swagger-ui.html |
| fleet-management-service | http://localhost:8084/swagger-ui.html |

The gateway's Swagger UI has a dropdown to switch between every service's spec.

## Deploy to Kubernetes with Istio

```bash
# Install Istio (demo profile) and label the namespace for sidecar injection
istioctl install --set profile=demo -y
kubectl create namespace connected-cars
kubectl label namespace connected-cars istio-injection=enabled

# Apply base, then Istio traffic + security policies
kubectl apply -f k8s/base/
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/istio/

# Verify mTLS + policies
istioctl authn tls-check api-gateway.connected-cars.svc.cluster.local
kubectl get authorizationpolicy -n connected-cars
```

Step-by-step with descriptions: [`docs/interview-prep/istio-service-mesh-guide.md`](docs/interview-prep/istio-service-mesh-guide.md) and [`docs/interview-prep/kubernetes-authorization-guide.md`](docs/interview-prep/kubernetes-authorization-guide.md).

## Provision AWS infrastructure

```bash
cd terraform
terraform init
terraform plan  -var-file=environments/dev.tfvars
terraform apply -var-file=environments/dev.tfvars
```

---

## Interview preparation

| Doc | Topic |
|---|---|
| [aws-architecture-interview.md](docs/interview-prep/aws-architecture-interview.md) | AWS reference architecture, trade-offs, Well-Architected pillars |
| [istio-service-mesh-guide.md](docs/interview-prep/istio-service-mesh-guide.md) | Service-to-service comms, mTLS, traffic management |
| [kubernetes-authorization-guide.md](docs/interview-prep/kubernetes-authorization-guide.md) | RBAC, AuthN/AuthZ, mesh authorization |
| [deloitte-mock-interview-qa.md](docs/interview-prep/deloitte-mock-interview-qa.md) | 40+ mock Q&A with model answers |
| [architecture-walkthrough-script.md](docs/interview-prep/architecture-walkthrough-script.md) | Word-for-word 5-minute spoken walkthrough |
| [resources-and-links.md](docs/interview-prep/resources-and-links.md) | Curated official docs, courses, whitepapers |
| [Connected-Cars-Architecture.pptx](docs/Connected-Cars-Architecture.pptx) | 13-slide architecture deck for the interview |

Run the full mesh locally on your laptop with kind: see [`k8s/local/README.md`](k8s/local/README.md) (`bash scripts/kind-up.sh`).

---

## License

MIT — for educational use.
