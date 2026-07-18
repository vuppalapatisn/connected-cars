# Deloitte AWS Architecture — Mock Interview Q&A Bank

40+ questions with model answers, grouped by theme. Practice answering out loud in 60–90 seconds each. Answers reference the Connected Cars project so you can ground abstract concepts in a real system.

---

## A. Warm-up / project deep-dive

**Q1. Walk me through the Connected Cars architecture.**
Use the 60-second pitch from [aws-architecture-interview.md](aws-architecture-interview.md#1-how-to-open-the-60-second-pitch), then draw the diagram. Lead with the *problem* (fleet-scale telemetry + real-time alerts + secure APIs), then the *shape* (IoT Core → EKS/Istio microservices → MSK → stores), then *cross-cutting* (security, observability, IaC).

**Q2. Why microservices and not a monolith?**
Independent scaling of the hot path (telemetry ingest scales to 20 pods while auth stays at 2), independent deploys, fault isolation, and team autonomy. Trade-off: distributed-systems complexity — which is exactly why we added a service mesh.

**Q3. What was the hardest problem you solved?**
Pick one and use STAR. Good candidates: enforcing zero-trust mTLS without breaking existing traffic (PERMISSIVE→STRICT migration), or handling telemetry bursts with Kafka back-pressure + KEDA autoscaling on consumer lag.

---

## B. Compute & EKS

**Q4. Why EKS over ECS or Lambda?**
EKS for the Kubernetes ecosystem, Istio mesh, portability, and IRSA. ECS is simpler ops but less flexible; Lambda is great for spiky, stateless glue but poor fit for long-lived stateful stream consumers and a mesh. We use Lambda for edge glue (e.g. IoT rule transforms).

**Q5. How do you scale the cluster?**
Two dimensions: **pods** via HPA (CPU/custom metrics/KEDA on Kafka lag) and **nodes** via Karpenter (or Cluster Autoscaler). Karpenter provisions right-sized, mixed Spot/On-Demand Graviton nodes just-in-time.

**Q6. Spot instances — how do you use them safely?**
Stateless workers (telemetry consumers, notification) on Spot with PodDisruptionBudgets and multiple instance types/AZs; stateful and control workloads on On-Demand. Handle interruption via the 2-min notice + graceful drain.

**Q7. How do pods get AWS permissions without static keys?**
**IRSA** — the ServiceAccount is annotated with an IAM role ARN; EKS's OIDC provider lets the pod assume that role via a projected token. Scoped, rotated, no secrets. (Newer: **EKS Pod Identity**.)

---

## C. Service mesh & networking

**Q8. How is service-to-service communication secured?**
Istio STRICT mTLS mesh-wide; each workload gets a SPIFFE identity from its ServiceAccount; `AuthorizationPolicy` enforces which service may call which. See [istio-service-mesh-guide.md](istio-service-mesh-guide.md).

**Q9. mTLS vs TLS — what's the difference and who manages the certs?**
TLS authenticates the server to the client; **mTLS** authenticates *both* ends. Istiod is the CA — it issues short-lived X.509 SPIFFE certs to each sidecar and rotates them automatically (default ~24h). Apps never handle certs.

**Q10. Difference between PeerAuthentication and RequestAuthentication?**
`PeerAuthentication` = transport/mTLS between *workloads* ("which service"). `RequestAuthentication` = end-user *JWT* validation ("which user"). Gotcha: RequestAuthentication doesn't *reject* token-less requests — an AuthorizationPolicy with `requestPrincipals: ["*"]` does.

**Q11. How do you do a canary release?**
Label pods with `version`, define `subsets` in a DestinationRule, and split weights in a VirtualService (90/10). Automate with Argo Rollouts, promoting on SLO metrics; auto-rollback on error-rate breach.

**Q12. Istio vs no mesh — justify the overhead.**
Mesh gives uniform mTLS, authz, retries/circuit-breaking, and observability with zero app code across languages. Cost: ~1–2ms/hop + sidecar resources + ops complexity. For a security-sensitive automotive platform with many services, worth it. Mention **ambient mesh** to cut overhead.

**Q13. NetworkPolicy vs AuthorizationPolicy?**
NetworkPolicy is L3/L4 (IP/port) enforced by the CNI; AuthorizationPolicy is L7 (identity, path, method, JWT claims) enforced by Envoy. Use both — defense in depth.

**Q14. How does traffic get from the internet to a pod?**
Route 53 → CloudFront/WAF → ALB (via AWS Load Balancer Controller) → Istio ingress gateway (TLS terminate) → VirtualService route → destination sidecar (mTLS) → app.

---

## D. Kubernetes authorization & security

**Q15. How is authorization configured in the cluster?**
Two planes: **RBAC** for the K8s API, **Istio AuthorizationPolicy** for app traffic. Full detail in [kubernetes-authorization-guide.md](kubernetes-authorization-guide.md).

**Q16. Explain RBAC objects.**
`Role`/`ClusterRole` define permissions (verbs on resources); `RoleBinding`/`ClusterRoleBinding` grant them to subjects (User/Group/ServiceAccount). Additive, default-deny.

**Q17. How do human users authenticate to EKS?**
No native users — IAM identities mapped to K8s users/groups via **EKS Access Entries** (or `aws-auth` CM). SSO via IAM Identity Center/OIDC. Groups then bound to Roles via RBAC.

**Q18. How do you enforce that only fleet-admins can modify vehicles?**
`RequestAuthentication` validates the JWT; an `AuthorizationPolicy` on the fleet service allows writes only `when request.auth.claims[roles]` contains `ROLE_FLEET_ADMIN`, and only from the api-gateway's mTLS principal.

**Q19. How do you keep secrets out of Git?**
External Secrets Operator syncs from AWS Secrets Manager into K8s Secrets (or CSI Secrets Store mounts them). IRSA grants the pod `secretsmanager:GetSecretValue` scoped by path.

**Q20. Pod security hardening?**
Pod Security Admission (`restricted`): non-root, read-only rootfs, drop capabilities, no privilege escalation; seccomp; distroless/alpine images; image scanning (Trivy) in CI; signed images (cosign); admission policy engine (Kyverno/Gatekeeper).

---

## E. Data & messaging

**Q21. Why Kafka/MSK over Kinesis or SQS?**
Kafka for high throughput, replay, ordered per-partition, mature ecosystem (Streams/Connect), no shard math. Kinesis if we wanted zero broker ops; SQS for simple point-to-point work queues, not a replayable event log. MSK Serverless splits the difference.

**Q22. How do you guarantee ordering?**
Partition by VIN — all events for one vehicle land on one partition and are consumed in order. Global ordering isn't needed (and doesn't scale).

**Q23. How do you handle a poison message?**
Consumer error handling with ret/backoff → dead-letter topic after N attempts; alert; reprocess after fix. Idempotent consumers so replays are safe.

**Q24. Which database for what?**
RDS/Aurora PostgreSQL for identity/registration/billing (ACID); Timestream (or Timescale) for telemetry time-series; ElastiCache Redis for hot state/dedup/rate-limits; S3 for cold/analytics.

**Q25. How do you scale the relational DB?**
Read replicas, Aurora auto-scaling, connection pooling (RDS Proxy), CQRS (write RDS, read cache/replica), and — most importantly — keep the firehose (telemetry) *out* of RDS.

**Q26. Exactly-once processing?**
Kafka idempotent producer + transactional writes where needed; idempotent consumers keyed on event id (dedup in Redis). Usually "effectively-once" via idempotency is enough and simpler.

---

## F. Reliability & operations

**Q27. What's your DR strategy / RTO / RPO?**
State targets first (e.g. RTO 1h, RPO 5min). Pilot-light: cross-region S3 replication, RDS cross-region snapshots/replica, IaC to stand up the standby, Route 53 failover. For tighter RTO, warm standby.

**Q28. How do you deploy safely?**
GitOps (Argo CD) from Git as source of truth; progressive delivery via Istio canaries + Argo Rollouts with automated analysis on SLOs; blue/green for risky changes; DB migrations via expand/contract.

**Q29. Observability stack?**
Metrics: Prometheus/AMP + Grafana (RED/USE + Istio golden signals). Traces: Jaeger/X-Ray (Istio auto-propagates). Logs: Loki/CloudWatch. Topology: Kiali. Alerts: Alertmanager → PagerDuty. SLOs with error budgets.

**Q30. How do you find a latency regression across services?**
Distributed tracing (Jaeger) to find the slow span; Kiali for the request path; correlate with Prometheus latency histograms and Envoy access logs.

---

## G. Cost & scale

**Q31. Telemetry storage is exploding — reduce cost?**
Tier it: hot in Timestream/Redis, warm in S3 Standard, cold in S3 Glacier via lifecycle; compress + columnar (Parquet) for analytics; sample non-critical signals; retention policies per signal class.

**Q32. Cut compute cost 30%?**
Graviton (ARM) instances, Spot for stateless workers, right-sizing via VPA recommendations, scale non-prod to zero off-hours, Savings Plans/Reserved for baseline, Karpenter consolidation.

**Q33. How would you handle 10M vehicles globally?**
Regional cells (IoT Core + EKS per region) for data residency and latency; Route 53 latency routing; async cross-region replication into a central analytics lake; shard by region/VIN.

---

## H. Behavioral / consulting (Deloitte-style)

**Q34. A client insists on a multi-cloud requirement. How do you respond?**
Clarify the *driver* (lock-in fear, regulatory, M&A). Offer portability where it's cheap (containers, K8s, Terraform, open standards) but be honest about the cost of lowest-common-denominator architectures; recommend abstraction only where it pays. Decisions tied to business value.

**Q35. Stakeholders disagree on build vs buy.**
Frame with TCO, time-to-market, differentiation (build what's core, buy what's commodity), and risk. Bring data, propose a spike/PoC, and make a recommendation — consultants decide, not just enumerate.

**Q36. How do you handle an unrealistic deadline?**
Scope negotiation: MVP vs later phases, surface risks early, quantify trade-offs, protect quality gates (security/tests). Communicate proactively.

**Q37. Tell me about a time you influenced without authority.**
STAR — e.g. drove the mesh adoption by running a PoC that showed mTLS + observability wins, then socializing metrics to get buy-in.

**Q38. Disagreed with a senior architect?**
Show respect + data: understand their reasoning, present trade-offs objectively, disagree-and-commit once decided.

---

## I. Curveballs

**Q39. Design a rate limiter for the vehicle API.**
Token-bucket in Redis keyed by client/VIN; enforce at the gateway (Spring Cloud Gateway RequestRateLimiter or Istio's local/global rate limiting via Envoy + a rate-limit service). Discuss distributed counters, clock skew, and fail-open vs fail-closed.

**Q40. A vehicle sends malformed/malicious telemetry. What happens?**
Validation at ingest (schema/JSON), WAF at edge, mTLS device certs (IoT Core) so only enrolled devices connect, anomaly detection on the stream, quarantine topic, and rate limiting per device. Never trust the edge.

**Q41. How do you push a security patch to the fleet (OTA)?**
Out of scope of this repo but: signed artifacts, staged rollout by cohort, rollback capability, device attestation, and audit — the same progressive-delivery philosophy as canaries, applied to vehicles.

**Q42. What would you change if you rebuilt this today?**
Evaluate **Istio ambient mode** (drop sidecars), **EKS Pod Identity** over IRSA, **MSK Serverless**, and more managed/serverless to cut ops. Shows you keep current.

---

## Delivery tips

- **Structure every answer:** context → decision → trade-off. 
- **Quantify** wherever you can.
- **Draw** — always ask for a whiteboard.
- **Volunteer trade-offs** before they're asked; it reads as senior.
- **It's OK to ask clarifying questions** about scale/SLAs before designing.
