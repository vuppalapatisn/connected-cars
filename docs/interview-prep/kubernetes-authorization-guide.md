# Kubernetes & Mesh Authorization — Step by Step

> Interview framing: *"How is authorization validated and configured in your Kubernetes cluster?"*

There are **two completely different authorization planes**, and a strong answer separates them explicitly:

| Plane | Controls | Mechanism | "Who" is the subject |
|---|---|---|---|
| **Control plane** | Who can act on the **Kubernetes API** (create pods, read secrets, scale deployments) | **RBAC** (`Role`/`ClusterRole` + bindings) | Humans & controllers (via OIDC/IAM), ServiceAccounts |
| **Data plane** | Which **service/user** can send **application traffic** to which service | **Istio `AuthorizationPolicy`** (+ `RequestAuthentication`) | Workload mTLS identities & end-user JWTs |

Getting these two confused is the #1 mistake. Say the sentence: *"RBAC governs the API server; Istio AuthorizationPolicy governs east-west application traffic."*

---

## Part A — Control-plane authorization (Kubernetes RBAC)

### A.1 The request lifecycle at the API server

Every call to the Kubernetes API passes through three gates, in order:

```
Request → [ Authentication ] → [ Authorization ] → [ Admission control ] → etcd
             who are you?          are you allowed?     is it well-formed/policy-compliant?
```

- **Authentication** on EKS: no built-in users. Identity comes from **AWS IAM** mapped to Kubernetes users/groups via **EKS Access Entries** (or the legacy `aws-auth` ConfigMap). Human SSO via IAM Identity Center / OIDC (Okta, Cognito).
- **Authorization**: the **RBAC** authorizer (default) checks whether the authenticated subject's roles permit the verb+resource.
- **Admission**: validating/mutating webhooks (e.g. OPA Gatekeeper, Kyverno, Pod Security Admission) enforce org policy.

### A.2 The four RBAC objects

| Object | Scope | Purpose |
|---|---|---|
| `Role` | namespace | a set of allowed verbs on resources |
| `ClusterRole` | cluster-wide | same, but cluster-scoped or reusable across namespaces |
| `RoleBinding` | namespace | grants a Role/ClusterRole to subjects in one namespace |
| `ClusterRoleBinding` | cluster-wide | grants a ClusterRole cluster-wide |

**Subjects** are `User`, `Group`, or `ServiceAccount`. RBAC is **purely additive and default-deny** — there are no "deny" rules; if nothing grants it, it's denied.

### A.3 Configuration (from [`k8s/base/rbac.yaml`](../../k8s/base/rbac.yaml))

Least-privilege Role letting only the telemetry SA read config:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata: { name: config-reader, namespace: connected-cars }
rules:
  - apiGroups: [""]
    resources: ["configmaps", "secrets"]
    verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata: { name: telemetry-config-reader, namespace: connected-cars }
subjects:
  - kind: ServiceAccount
    name: vehicle-telemetry-service
    namespace: connected-cars
roleRef:
  kind: Role
  name: config-reader
  apiGroup: rbac.authorization.k8s.io
```

A read-only role for the human on-call group (group name comes from your OIDC provider):

```yaml
kind: RoleBinding
subjects:
  - kind: Group
    name: connected-cars-operators   # mapped from IAM/OIDC
roleRef: { kind: Role, name: fleet-operator-readonly, apiGroup: rbac.authorization.k8s.io }
```

### A.4 Validate RBAC

```bash
# Can the telemetry SA read secrets?  (expect: yes)
kubectl auth can-i get secrets \
  --as=system:serviceaccount:connected-cars:vehicle-telemetry-service -n connected-cars

# Can it delete deployments?  (expect: no)
kubectl auth can-i delete deployments \
  --as=system:serviceaccount:connected-cars:vehicle-telemetry-service -n connected-cars
```

### A.5 RBAC best practices to cite

- One **ServiceAccount per workload**; never use the `default` SA; set `automountServiceAccountToken: false` where the pod doesn't call the API.
- Prefer namespaced `Role` over `ClusterRole`; avoid wildcards (`verbs: ["*"]`, `resources: ["*"]`).
- Never bind humans to `cluster-admin`; use groups + break-glass.
- Add **Pod Security Admission** (`restricted`) and a policy engine (Kyverno/Gatekeeper) at the admission gate.
- Pods reach AWS via **IRSA** (SA annotated with an IAM role) — not node instance profiles, not static keys.

---

## Part B — Data-plane authorization (Istio)

This is where "service A may call service B, and only fleet-admins may write" is enforced. Two resources work together.

### B.1 RequestAuthentication — validate the end-user JWT

From [`k8s/istio/request-authentication.yaml`](../../k8s/istio/request-authentication.yaml):

```yaml
apiVersion: security.istio.io/v1
kind: RequestAuthentication
metadata: { name: jwt-at-ingress, namespace: connected-cars }
spec:
  selector: { matchLabels: { app: api-gateway } }
  jwtRules:
    - issuer: "https://auth.connected-cars.toyota.com"
      jwksUri: "http://auth-service.connected-cars.svc.cluster.local:8081/.well-known/jwks.json"
      audiences: ["connected-cars-api"]
      forwardOriginalToken: true
```

⚠️ **Critical gotcha:** `RequestAuthentication` only *validates a token when one is present*. A request **with no token still passes** this resource. To actually *require* a token you must add an `AuthorizationPolicy` that demands `requestPrincipals: ["*"]`. Interviewers love this one.

### B.2 AuthorizationPolicy — the allow/deny rules

Evaluation order Envoy applies **per request**: `CUSTOM` → `DENY` → `ALLOW`. If any ALLOW policy selects a workload, requests must match one (default-deny for that workload). From [`k8s/istio/authorization-policy.yaml`](../../k8s/istio/authorization-policy.yaml):

**1) Namespace default-deny (zero trust):**
```yaml
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata: { name: default-deny-all, namespace: connected-cars }
spec: {}          # empty = deny everything until explicitly allowed
```

**2) Allow only the api-gateway to call the fleet service, with role-based writes:**
```yaml
spec:
  selector: { matchLabels: { app: fleet-management-service } }
  action: ALLOW
  rules:
    - from:
        - source:
            principals: ["cluster.local/ns/connected-cars/sa/api-gateway"]  # mTLS identity
            requestPrincipals: ["*"]                                        # a valid JWT is required
      to:
        - operation: { methods: ["POST","PUT","PATCH","DELETE"] }
      when:
        - key: request.auth.claims[roles]
          values: ["ROLE_FLEET_ADMIN"]                                      # JWT claim check
    - from:
        - source:
            principals: ["cluster.local/ns/connected-cars/sa/api-gateway"]
            requestPrincipals: ["*"]
      to:
        - operation: { methods: ["GET"] }                                   # reads for any authed user
```

This single policy demonstrates the three authorization dimensions in one breath:
- **`principals`** → *which service* (mTLS/SPIFFE identity, from the ServiceAccount).
- **`requestPrincipals`** → *authenticated end-user* (must have a valid JWT).
- **`when` on `request.auth.claims`** → *which role* (fine-grained, claim-based).

### B.3 Validate mesh authorization

```bash
kubectl get authorizationpolicy -n connected-cars
istioctl proxy-config log deploy/fleet-management-service -n connected-cars --level rbac:debug
# then tail the sidecar to see ALLOW/DENY decisions:
kubectl logs deploy/fleet-management-service -c istio-proxy -n connected-cars | grep rbac
```

Functional test:
```bash
# fleet-admin token can create a vehicle -> 201
curl -X POST https://api.connected-cars.toyota.com/api/v1/fleet/vehicles \
  -H "Authorization: Bearer $ADMIN_JWT" -H 'Content-Type: application/json' \
  -d '{"vin":"JT001","model":"bZ4X","modelYear":2025,"owner":"acme"}'

# driver token attempting the same write -> 403 RBAC: access denied
curl -X POST ... -H "Authorization: Bearer $DRIVER_JWT" ...
```

---

## Part C — Defense in depth: how the layers stack

```
AWS WAF/ALB  →  Istio Gateway (TLS)  →  RequestAuthentication (JWT)  →  AuthorizationPolicy (svc+role)
                                                                         │
Kubernetes RBAC (who can touch the API)  ─────────────── orthogonal ────┘
NetworkPolicy (L3/L4 pod firewall)  ·  IRSA (pod→AWS IAM)  ·  Pod Security Admission
```

Mention **NetworkPolicy** too: it's the L3/L4 firewall between pods (CNI-enforced, e.g. Calico/VPC-CNI), complementary to Istio's L7 authorization. Belt and suspenders.

---

## One-line summary to memorize

> *"Two planes: Kubernetes **RBAC** authorizes actions on the **API server** (subjects come from AWS IAM/OIDC and ServiceAccounts, default-deny, additive). Istio **AuthorizationPolicy** authorizes **application traffic** based on workload **mTLS identity** (from ServiceAccounts) plus end-user **JWT claims**, on top of a namespace default-deny. Pods reach AWS via **IRSA**, and **NetworkPolicy** adds an L3/L4 firewall."*
