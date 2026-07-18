# AWS Architecture Interview — Connected Cars (Toyota)

A structured walkthrough you can deliver in 5–7 minutes, then defend under follow-ups. Tailored for a **Deloitte AWS solution-architecture** interview.

---

## 1. How to open (the 60-second pitch)

> "Connected Cars is an event-driven telemetry platform. Vehicles stream telemetry over MQTT/HTTPS into AWS IoT Core, which fronts an EKS-hosted set of Spring Boot microservices meshed with Istio. Telemetry is durably buffered in Amazon MSK (Kafka), processed for real-time alerting, and persisted to purpose-built stores — RDS PostgreSQL for identity/registration, a time-series store for telemetry, ElastiCache for hot state. Everything is provisioned with Terraform, secured with mTLS + IAM + WAF, and observed with Prometheus/Grafana/Jaeger. It's designed against the AWS Well-Architected Framework."

Then draw the diagram (see [`ARCHITECTURE.md`](../../ARCHITECTURE.md)).

---

## 2. Requirements you should state before designing (they test this)

**Functional:** ingest telemetry; real-time alerts (crash, geofence, low battery, DTC); driver/dealer/fleet APIs; OTA-adjacent command & control; historical analytics.

**Non-functional (the differentiator):**
- **Scale:** ~10M vehicles × events/min → hundreds of thousands of msgs/sec peak.
- **Latency:** alerts p99 < 2s end-to-end.
- **Availability:** 99.95%+, multi-AZ, regional DR.
- **Security/compliance:** PII, GDPR/CCPA, automotive cyber regs (UNECE R155/R156, ISO/SAE 21434).
- **Cost:** telemetry is huge — tiered storage & Graviton/spot matter.

Always convert vague asks into numbers; it signals seniority.

---

## 3. The reference architecture, component by component

| Concern | AWS service | Why / alternatives |
|---|---|---|
| Device connectivity | **AWS IoT Core** (MQTT, X.509 device certs) | Millions of persistent connections, per-device identity, rules engine. Alt: self-managed EMQX. |
| Edge / API | **ALB + AWS WAF + CloudFront** | TLS, WAF rules, DDoS (Shield). CloudFront for mobile API/CDN. |
| Compute | **Amazon EKS** | Managed K8s + Istio + IRSA. Alt: ECS (simpler), Lambda (spiky/stateless). |
| Service mesh | **Istio** on EKS | mTLS, authz, traffic mgmt (see the mesh guide). Alt: App Mesh (AWS-native, being deprecated), Linkerd. |
| Event backbone | **Amazon MSK (Kafka)** | Durable, replayable, high-throughput, ordered per partition. Alt: Kinesis Data Streams (simpler ops, shard limits), MSK Serverless. |
| Stream processing | **Kafka Streams / Flink (KDA)** | Windowed aggregations, alert rules. |
| Relational | **RDS/Aurora PostgreSQL (Multi-AZ)** | Identity, registration, billing — strong consistency. |
| Time-series telemetry | **Amazon Timestream** (or Timescale on RDS) | Purpose-built, retention tiers, cheap at scale. |
| Hot state / cache | **ElastiCache Redis** | Dedup, rate limits, session/notification throttling. |
| Object / data lake | **S3 (tiered) + Athena/Glue** | Cold telemetry, analytics, ML feature store. |
| Notifications | **SNS / SES / Pinpoint** | Push/SMS/email fan-out. |
| Secrets | **Secrets Manager + External Secrets Operator** | Rotation; no secrets in Git. |
| IaC | **Terraform** | Multi-account, modules, remote state. Alt: CDK. |
| Observability | **Managed Prometheus/Grafana (AMP/AMG), X-Ray/Jaeger, CloudWatch** | Metrics, traces, logs. |

---

## 4. Data flow (narrate this)

1. Vehicle → **IoT Core** (MQTT, mutual X.509). IoT Rule forwards to the ingest path.
2. **api-gateway** (or IoT Rule → MSK directly) validates and hands off to **vehicle-telemetry-service**.
3. Telemetry service **persists** (time-series) and **publishes** to **MSK** topic `vehicle.telemetry`, partitioned by VIN (preserves per-vehicle ordering).
4. **notification-service** consumes, evaluates alert rules, and dispatches via **SNS/Pinpoint**.
5. **fleet-management-service** and **auth-service** back the customer/dealer APIs off **RDS**.
6. Cold telemetry tiers to **S3**; **Athena/Glue/EMR** power analytics & ML.

---

## 5. The Well-Architected story (they will ask for at least two pillars)

- **Reliability:** Multi-AZ everywhere; MSK replication factor 3; RDS Multi-AZ + read replicas; HPA + Karpenter; Istio retries/outlier detection; PodDisruptionBudgets; cross-region DR (pilot-light: replicate S3 + RDS snapshots, IaC to stand up the standby region).
- **Security:** Zero-trust mesh (STRICT mTLS), least-privilege IAM/RBAC, IRSA, WAF/Shield, KMS encryption at rest, Secrets Manager, private subnets, VPC endpoints, GuardDuty/Security Hub.
- **Performance Efficiency:** Kafka partitioning for parallelism, caching, Graviton nodes, right-sized instance types, async pipeline decoupling ingest from processing.
- **Cost Optimization:** Graviton + Spot for stateless workers, S3 Intelligent-Tiering for cold telemetry, autoscale non-prod to zero, MSK/Kinesis sizing, savings plans.
- **Operational Excellence:** Terraform + GitOps (Argo CD), progressive delivery via Istio canaries, runbooks, golden dashboards, chaos testing.
- **Sustainability:** Graviton (ARM efficiency), spot, scale-to-zero, data lifecycle policies.

---

## 6. Scaling & bottleneck questions (rapid-fire prep)

- **"How do you handle 10x telemetry spikes?"** → Kafka absorbs bursts (buffer); consumers scale via consumer-group + HPA on lag (KEDA); ingest is stateless behind the mesh; back-pressure is natural because producers write to Kafka, not to downstream services.
- **"Hot partition / one noisy VIN?"** → key by VIN generally, but for hot keys add a compound key or salt; monitor partition skew.
- **"Exactly-once alerts?"** → idempotent consumers keyed on event id in Redis; Kafka transactions if needed; dedup window.
- **"Global fleet, low latency worldwide?"** → regional cells, IoT Core per region, Route 53 latency routing, data residency per region, async cross-region replication for analytics.
- **"RDS is the bottleneck."** → read replicas, Aurora, CQRS (writes to RDS, reads from a cache/replica), move telemetry off RDS to Timestream.

---

## 7. Trade-offs to volunteer (seniority signal)

- **EKS vs ECS vs Lambda:** EKS chosen for mesh + portability + ecosystem, at the cost of operational complexity. Lambda for spiky glue; ECS if the org wanted less K8s ops.
- **MSK vs Kinesis:** MSK for Kafka ecosystem/replay/throughput and no shard math; Kinesis if we wanted zero broker ops. MSK Serverless splits the difference.
- **Istio overhead:** ~1–2ms/hop + sidecar CPU/mem; mitigated by ambient mode.
- **Self-managed time-series vs Timestream:** managed reduces ops but has query-cost/portability considerations.

---

## 8. Failure-scenario drills

- **AZ outage:** Multi-AZ node groups + data replicas ride it out; capacity headroom via Karpenter.
- **Region outage:** DR runbook — promote replicated RDS, redeploy via Terraform, repoint Route 53. State RTO/RPO targets (e.g. RTO 1h, RPO 5min).
- **Kafka lag climbing:** scale consumers (KEDA on lag), add partitions, shed non-critical processing.
- **Bad deploy:** Istio canary caught it at 10% weight → auto-rollback via Argo Rollouts on SLO breach.

---

## 9. Closing framework: STAR for the project story

- **Situation:** Toyota needed a scalable, secure connected-vehicle platform.
- **Task:** design service-to-service security + real-time alerting at fleet scale.
- **Action:** EKS + Istio (mTLS/authz), MSK event backbone, Terraform IaC, Well-Architected.
- **Result:** (quantify) e.g. "handled N events/sec at p99 < 2s, zero plaintext east-west traffic, cut alert latency by X%, reduced infra cost Y% via Graviton/Spot."

Have 2–3 quantified results ready — even approximate numbers beat none.

---

See the full [mock Q&A bank →](deloitte-mock-interview-qa.md).
