# Interview Preparation — Index

Everything you need for the Deloitte AWS architecture mock interview, grounded in the Connected Cars (Toyota) project.

## Read in this order

1. **[aws-architecture-interview.md](aws-architecture-interview.md)** — the reference architecture, the 60-second pitch, Well-Architected mapping, scaling/failure drills, and trade-offs. *Start here.*
2. **[istio-service-mesh-guide.md](istio-service-mesh-guide.md)** — service-to-service communication, mTLS, and traffic management, step by step with the actual YAML.
3. **[kubernetes-authorization-guide.md](kubernetes-authorization-guide.md)** — the two authorization planes: Kubernetes RBAC and Istio AuthorizationPolicy, with validation commands.
4. **[deloitte-mock-interview-qa.md](deloitte-mock-interview-qa.md)** — 40+ mock questions with model answers (technical + behavioral/consulting).
5. **[resources-and-links.md](resources-and-links.md)** — curated official docs, whitepapers, and a print-ready revision checklist.

## The three sentences to memorize

1. **Service-to-service:** *"Istio gives every workload a SPIFFE identity from its ServiceAccount and enforces STRICT mTLS, so all east-west traffic is encrypted and mutually authenticated with no app code."*
2. **Authorization:** *"Two planes — Kubernetes RBAC authorizes actions on the API server; Istio AuthorizationPolicy authorizes application traffic using workload mTLS identity plus end-user JWT claims, on a namespace default-deny."*
3. **Why AWS the way we did:** *"EKS for the mesh + IRSA, MSK for a durable replayable event backbone, purpose-built stores per data shape, Terraform + GitOps for delivery — all mapped to the Well-Architected pillars."*

## Practice plan (1 week)

| Day | Focus |
|---|---|
| 1 | Draw the architecture from memory; deliver the 60s pitch out loud 5×. |
| 2 | Istio: mTLS cert lifecycle, PeerAuth vs RequestAuth vs AuthzPolicy. |
| 3 | Kubernetes: RBAC objects, EKS IAM mapping, IRSA flow. |
| 4 | Data/messaging: Kafka partitioning, ordering, autoscaling on lag. |
| 5 | Well-Architected + DR (RTO/RPO) + cost optimization. |
| 6 | Behavioral/consulting answers (STAR) with quantified results. |
| 7 | Full mock: 45 min, whiteboard, record yourself. |
