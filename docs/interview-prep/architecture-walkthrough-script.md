# The 5-Minute Spoken Architecture Walkthrough (Script)

A word-for-word script to deliver when the interviewer says *"Walk me through the architecture."* Timed to ~5 minutes at a natural pace (~750 words). Practice it out loud until it's yours — then adapt, don't recite.

**Delivery notes:** ask for a whiteboard, draw as you talk, pause after each section for questions, and land the trade-offs — those are what senior interviewers listen for. Bracketed `[draw: …]` cues tell you what to sketch.

---

## [0:00–0:30] Frame the problem

> "Let me start with the problem, because it drives every decision. Connected Cars is Toyota's platform for vehicles that continuously stream telemetry — location, speed, battery, and diagnostic trouble codes. We're talking on the order of millions of vehicles, so hundreds of thousands of messages per second at peak. The platform has to do three things: ingest that firehose reliably, turn it into near-real-time alerts — a crash or a low battery in under two seconds — and expose secure APIs to mobile apps, dealers, and fleet operators. And because it's automotive, security and compliance are first-class: PII, plus regs like UNECE R155 and ISO 21434."

## [0:30–1:15] The shape of the system

> "[draw: left-to-right — vehicles → AWS box → EKS box → data stores] At the edge, vehicles connect over MQTT to AWS IoT Core, which gives us per-device X.509 identity and handles millions of persistent connections. Mobile and dealer traffic comes in through CloudFront and an ALB with WAF in front.
>
> Everything then lands on Amazon EKS, where the application is a set of Spring Boot microservices. I chose EKS over ECS or Lambda specifically because I wanted a service mesh and fine-grained IAM through IRSA — I'll come back to both. The services are an API gateway, an auth service, a telemetry ingest service, a notification service, and a fleet-management service."

## [1:15–2:15] The data flow

> "[draw: telemetry → Kafka → notification; telemetry → time-series] Here's the critical path. A vehicle's telemetry hits the ingest service, which does two things: it persists to a time-series store — Timestream — and it publishes to Amazon MSK, our Kafka backbone, keyed by VIN. That key matters: it keeps every vehicle's events ordered on one partition, and it lets me scale consumers horizontally.
>
> The notification service consumes that stream, evaluates alert rules — low battery, overspeed, a diagnostic code — and fans out through SNS and Pinpoint. The key architectural point is that Kafka decouples ingest from processing. If we get a 10x spike, Kafka absorbs the burst as a buffer, and the consumers scale out on lag using KEDA. Producers never block on downstream services. [draw: RDS under auth + fleet] The auth and fleet services back the customer APIs off RDS Postgres, which is Multi-AZ for the data that needs strong consistency — identity, registration, billing. So: purpose-built stores per data shape — Timestream for telemetry, Postgres for relational, Redis for hot state, S3 for cold analytics."

## [2:15–3:30] Service-to-service security — the mesh

> "[draw: two pods each with an Envoy sidecar, lock between them] Now the part I'm most proud of — securing service-to-service communication. Every pod runs an Istio Envoy sidecar. Istio's control plane issues each workload a short-lived SPIFFE X.509 identity, derived from its Kubernetes ServiceAccount, and rotates it automatically. We run STRICT mutual TLS mesh-wide, so all east-west traffic is encrypted and *mutually* authenticated — with zero application code. The apps still just speak plain HTTP to localhost; the sidecars transparently upgrade it.
>
> On top of that, authorization is declarative. An Istio AuthorizationPolicy says exactly which service may call which — for example, only the API gateway's identity may reach the fleet service. And it combines two dimensions: the calling *service's* mTLS identity, and the end-*user's* JWT claims. So a rule can say 'only the gateway, and only for a user whose token carries the fleet-admin role, may issue a write.' We run a namespace default-deny, so it's genuinely zero-trust — nothing talks to anything until explicitly allowed."

## [3:30–4:15] Cluster authorization & the two planes

> "It's worth separating two authorization planes, because they get conflated. Kubernetes RBAC governs the *API server* — who can create pods or read secrets — and on EKS those identities come from AWS IAM mapped to Kubernetes groups. That's completely separate from the Istio policies, which govern *application traffic* in the data plane. And pods get their AWS permissions through IRSA — the ServiceAccount is annotated with an IAM role, so a pod assumes a scoped role with no long-lived keys at all. Belt and suspenders: we also run NetworkPolicies as an L3/L4 firewall underneath the L7 mesh."

## [4:15–5:00] Cross-cutting + close with trade-offs

> "Tying it together: it's all Terraform for infrastructure and GitOps with Argo CD for delivery, including progressive canary releases through Istio traffic weights. Observability is Prometheus and Grafana for metrics, Jaeger for traces — which the mesh propagates for free — and Kiali for topology.
>
> I'd map the whole thing to the Well-Architected pillars, and I'll call out the honest trade-offs: the mesh adds a millisecond or two per hop and real operational complexity — for a smaller system I wouldn't reach for Istio, and today I'd evaluate its ambient, sidecar-less mode to cut that overhead. I picked MSK over Kinesis for replay and the Kafka ecosystem, at the cost of managing brokers. And we run on Graviton with Spot for the stateless workers to keep the telemetry bill sane. Happy to go deeper anywhere — the mesh security, the scaling model, or the DR strategy."

---

## Cheat-sheet (the beats, if you blank)

1. **Problem** — millions of vehicles, real-time alerts <2s, secure APIs, automotive compliance.
2. **Shape** — IoT Core → EKS Spring Boot microservices → purpose-built stores.
3. **Data flow** — ingest → Kafka (by VIN) → notification; Kafka decouples + buffers.
4. **Mesh** — Envoy sidecars, SPIFFE identity, STRICT mTLS, AuthorizationPolicy (service identity + JWT role), default-deny.
5. **Two planes** — RBAC (API server, IAM-mapped) vs Istio authz (traffic); IRSA; NetworkPolicy.
6. **Close** — Terraform/GitOps/canary, observability, and volunteer trade-offs (mesh overhead, MSK vs Kinesis, Graviton/Spot).

## Likely immediate follow-ups (be ready)

- *"What happens on a 10x spike?"* → Kafka buffer + KEDA on consumer lag; producers don't block.
- *"How are the mTLS certs managed?"* → istiod is the CA; short-lived SPIFFE certs, auto-rotated.
- *"RBAC vs AuthorizationPolicy?"* → API-server actions vs application traffic; different planes.
- *"Why EKS not Lambda?"* → mesh + long-lived stream consumers + portability; Lambda for edge glue.
- *"DR?"* → state RTO/RPO, pilot-light: cross-region S3/RDS replication + Terraform + Route 53 failover.
