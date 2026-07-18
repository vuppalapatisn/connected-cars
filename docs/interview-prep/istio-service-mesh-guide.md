# Istio Service Mesh — Service-to-Service Communication (Step by Step)

> Interview framing: *"On the Toyota Connected Cars program, how did you secure and manage service-to-service communication?"*
>
> Short answer: **We ran the microservices on Amazon EKS with an Istio service mesh. Every pod gets an Envoy sidecar. Istio issues each workload a short-lived SPIFFE X.509 identity and enforces STRICT mutual TLS, so all service-to-service traffic is encrypted and mutually authenticated with zero application code. Authorization is expressed declaratively with `AuthorizationPolicy` (which service may call which, on which path/method) and `RequestAuthentication` (end-user JWT validation). Traffic management — retries, timeouts, circuit breaking, and canary releases — is handled by `VirtualService` and `DestinationRule`.**

---

## 1. Mental model: how a call flows through the mesh

```
auth-service pod                                  fleet-service pod
┌────────────────────┐                         ┌────────────────────┐
│  app container      │                         │  app container      │
│  (localhost:8081)   │                         │  (localhost:8084)   │
│        │            │                         │        ▲            │
│        ▼            │   mTLS (Envoy↔Envoy)    │        │            │
│  Envoy sidecar  ────┼─────────────────────────┼──►  Envoy sidecar  │
│  (outbound)         │   SPIFFE cert exchange  │  (inbound)          │
└────────────────────┘                         └────────────────────┘
        ▲                                                ▲
        └── PeerAuthentication (mTLS mode)               └── AuthorizationPolicy (allow/deny)
            DestinationRule (client TLS, LB)                 RequestAuthentication (JWT)
            VirtualService (routing)
```

Key point for interviews: **the application containers still speak plain HTTP to `localhost`. The sidecars transparently upgrade the connection to mTLS.** No code change, no library, no cert handling in the app.

---

## 2. The four resource types you must be able to name

| Resource | Layer | Answers the question |
|---|---|---|
| `PeerAuthentication` | Transport / mTLS | *Which **service** is calling? Is the channel encrypted?* |
| `RequestAuthentication` | End-user | *Which **user** made this request? Is the JWT valid?* |
| `AuthorizationPolicy` | Access control | *Is this caller **allowed** to do this operation?* |
| `VirtualService` + `DestinationRule` | Traffic | *Where does the request **go**, and with what resilience?* |

---

## 3. Step-by-step configuration

### Step 0 — Install Istio and enable sidecar injection

```bash
istioctl install --set profile=demo -y
kubectl create namespace connected-cars
kubectl label namespace connected-cars istio-injection=enabled
```

The label is what triggers the sidecar-injector webhook to add an Envoy container to every new pod. Verify:

```bash
kubectl get pod -n connected-cars -o jsonpath='{.items[*].spec.containers[*].name}'
# expect: <app> istio-proxy  (2 containers per pod)
```

### Step 1 — Turn on mutual TLS (PeerAuthentication)

See [`k8s/istio/peer-authentication.yaml`](../../k8s/istio/peer-authentication.yaml).

```yaml
apiVersion: security.istio.io/v1
kind: PeerAuthentication
metadata:
  name: default
  namespace: istio-system   # root namespace = mesh-wide default
spec:
  mtls:
    mode: STRICT            # reject any non-mTLS (plaintext) traffic
```

- `STRICT` — only mutually-authenticated TLS accepted (production target).
- `PERMISSIVE` — accept both mTLS and plaintext (use during migration).
- `DISABLE` — no mТLS.

**Migration tip (common question):** roll out as `PERMISSIVE` first so existing plaintext clients keep working, confirm 100% of traffic is mTLS in Kiali, then flip to `STRICT`. Answering this shows real operational experience.

Verify what mode a workload negotiates:

```bash
istioctl authn tls-check api-gateway.connected-cars.svc.cluster.local
```

### Step 2 — Set client-side policy (DestinationRule)

See [`k8s/istio/destination-rule.yaml`](../../k8s/istio/destination-rule.yaml).

```yaml
trafficPolicy:
  tls:
    mode: ISTIO_MUTUAL          # caller presents its Istio-issued cert
  outlierDetection:             # circuit breaking
    consecutive5xxErrors: 5
    baseEjectionTime: 30s
  connectionPool:
    http: { http2MaxRequests: 1000 }
subsets:
  - name: v1
    labels: { version: v1 }
  - name: v2
    labels: { version: v2 }
```

`subsets` group pods by the `version` label — the building block for canary releases.

### Step 3 — Route traffic (VirtualService)

See [`k8s/istio/virtual-service.yaml`](../../k8s/istio/virtual-service.yaml). This is where you configure **retries, timeouts, and traffic splitting**:

```yaml
http:
  - route:
      - destination: { host: vehicle-telemetry-service, subset: v1 }
        weight: 90
      - destination: { host: vehicle-telemetry-service, subset: v2 }
        weight: 10           # canary 10% to v2
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: gateway-error,connect-failure
    timeout: 10s
```

### Step 4 — Identity is derived from the ServiceAccount

This is the linchpin most candidates miss. Each workload runs under a Kubernetes **ServiceAccount** (see [`k8s/base/serviceaccounts.yaml`](../../k8s/base/serviceaccounts.yaml)). Istio mints the SPIFFE identity from it:

```
spiffe://cluster.local/ns/connected-cars/sa/api-gateway
```

That string is exactly what you reference as a `principal` in an `AuthorizationPolicy`. **No ServiceAccount separation → no meaningful service-level authorization.** So give every service its own SA.

---

## 4. How the pieces cooperate (say this out loud in the interview)

1. A request hits the **Istio ingress gateway** (behind an AWS ALB), TLS-terminated per [`gateway.yaml`](../../k8s/istio/gateway.yaml).
2. `RequestAuthentication` validates the end-user **JWT** (signature/issuer/audience/expiry against JWKS).
3. `AuthorizationPolicy` decides if the request is **allowed** — based on the calling service's mTLS identity **and** the JWT claims (e.g. `roles`).
4. `VirtualService` routes it; `DestinationRule` applies mTLS + resilience.
5. The receiving sidecar terminates mTLS and forwards plain HTTP to the app on localhost.

---

## 5. Common failure modes & how you debug them

| Symptom | Likely cause | Command |
|---|---|---|
| `503 UC`/`connection reset` after enabling STRICT | a client still sends plaintext | `istioctl authn tls-check <host>` |
| `RBAC: access denied` | an `AuthorizationPolicy` denies it (or default-deny with no matching allow) | `istioctl proxy-config log <pod> --level rbac:debug` then check Envoy logs |
| JWT rejected | wrong issuer/audience or unreachable JWKS | check `RequestAuthentication`, curl the `jwksUri` from a pod |
| Canary sends 100% to v1 | pods missing the `version` label / no `subsets` | `kubectl get pods --show-labels` |

---

## 6. Why a mesh instead of doing this in the app?

- **Uniform, language-agnostic security** — Java, Go, Node all get mTLS identically.
- **Zero-trust by default** — encryption + identity for east-west traffic without trusting the network.
- **Separation of concerns** — platform team owns security/traffic policy; app teams ship features.
- **Observability for free** — golden metrics, distributed traces, and topology (Kiali) with no instrumentation.

Trade-offs to acknowledge (shows maturity): extra latency (~1–2ms/hop), sidecar resource overhead, and operational complexity. Mention **Istio ambient mode (sidecar-less)** as the emerging answer to the overhead concern.

---

Continue to [Kubernetes Authorization guide →](kubernetes-authorization-guide.md)
