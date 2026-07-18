# Curated Resources & Links

Official documentation, whitepapers, and study material for the AWS architecture + Kubernetes/Istio interview. Prefer primary sources (docs/whitepapers) over blogs.

## AWS Well-Architected & architecture
- AWS Well-Architected Framework — https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html
- Well-Architected Tool — https://aws.amazon.com/well-architected-tool/
- AWS Architecture Center — https://aws.amazon.com/architecture/
- AWS Solutions Library — https://aws.amazon.com/solutions/
- This Is My Architecture (video series) — https://aws.amazon.com/architecture/this-is-my-architecture/
- AWS Prescriptive Guidance — https://aws.amazon.com/prescriptive-guidance/

## AWS core services
- Amazon EKS docs — https://docs.aws.amazon.com/eks/latest/userguide/what-is-eks.html
- EKS Best Practices Guide — https://aws.github.io/aws-eks-best-practices/
- IRSA (IAM roles for service accounts) — https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts.html
- EKS Pod Identity — https://docs.aws.amazon.com/eks/latest/userguide/pod-identities.html
- Amazon MSK — https://docs.aws.amazon.com/msk/latest/developerguide/what-is-msk.html
- Amazon Kinesis vs MSK guidance — https://aws.amazon.com/kinesis/data-streams/faqs/
- AWS IoT Core — https://docs.aws.amazon.com/iot/latest/developerguide/what-is-aws-iot.html
- Amazon Timestream — https://docs.aws.amazon.com/timestream/latest/developerguide/what-is-timestream.html
- RDS/Aurora — https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Welcome.html
- AWS IoT for Automotive / Connected Mobility Solution — https://aws.amazon.com/automotive/

## Kubernetes
- Kubernetes docs — https://kubernetes.io/docs/home/
- RBAC — https://kubernetes.io/docs/reference/access-authn-authz/rbac/
- Authorization overview — https://kubernetes.io/docs/reference/access-authn-authz/authorization/
- Controlling access to the API — https://kubernetes.io/docs/concepts/security/controlling-access/
- Service Accounts — https://kubernetes.io/docs/concepts/security/service-accounts/
- Network Policies — https://kubernetes.io/docs/concepts/services-networking/network-policies/
- Pod Security Standards — https://kubernetes.io/docs/concepts/security/pod-security-standards/
- HPA — https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/

## Istio service mesh
- Istio docs — https://istio.io/latest/docs/
- Security concepts — https://istio.io/latest/docs/concepts/security/
- Mutual TLS / PeerAuthentication — https://istio.io/latest/docs/tasks/security/authentication/authn-policy/
- Request auth (JWT) — https://istio.io/latest/docs/tasks/security/authentication/authn-policy/#end-user-authentication
- Authorization Policy — https://istio.io/latest/docs/reference/config/security/authorization-policy/
- Authorization tasks — https://istio.io/latest/docs/tasks/security/authorization/
- Traffic management — https://istio.io/latest/docs/concepts/traffic-management/
- Ambient mesh (sidecar-less) — https://istio.io/latest/docs/ambient/
- SPIFFE/SPIRE identity — https://spiffe.io/docs/latest/spiffe-about/overview/

## Infrastructure as code & GitOps
- Terraform AWS provider — https://registry.terraform.io/providers/hashicorp/aws/latest/docs
- terraform-aws-modules/eks — https://registry.terraform.io/modules/terraform-aws-modules/eks/aws/latest
- terraform-aws-modules/vpc — https://registry.terraform.io/modules/terraform-aws-modules/vpc/aws/latest
- Argo CD — https://argo-cd.readthedocs.io/
- Argo Rollouts (progressive delivery) — https://argoproj.github.io/argo-rollouts/
- External Secrets Operator — https://external-secrets.io/
- Karpenter — https://karpenter.sh/

## Observability
- Prometheus — https://prometheus.io/docs/introduction/overview/
- Amazon Managed Prometheus/Grafana — https://aws.amazon.com/prometheus/ , https://aws.amazon.com/grafana/
- Kiali — https://kiali.io/
- Jaeger — https://www.jaegertracing.io/docs/
- OpenTelemetry — https://opentelemetry.io/docs/

## Spring / Java microservices
- Spring Boot — https://docs.spring.io/spring-boot/index.html
- Spring Cloud Gateway — https://docs.spring.io/spring-cloud-gateway/reference/
- Spring for Apache Kafka — https://docs.spring.io/spring-kafka/reference/
- Spring Security — https://docs.spring.io/spring-security/reference/

## Automotive / connected-vehicle context
- ISO/SAE 21434 (road vehicle cybersecurity) — https://www.iso.org/standard/70918.html
- UNECE R155 (cyber security management) — https://unece.org/transport/documents/2021/03/standards/un-regulation-no-155-cyber-security-and-cyber-security
- AWS Connected Mobility — https://aws.amazon.com/solutions/implementations/aws-connected-mobility-solution/

## Practice / mock interviews
- AWS Skill Builder — https://skillbuilder.aws/
- AWS Certified Solutions Architect – Professional (exam guide) — https://aws.amazon.com/certification/certified-solutions-architect-professional/
- "System Design Interview" by Alex Xu (Vol 1 & 2) — book
- Kubernetes the Hard Way (Kelsey Hightower) — https://github.com/kelseyhightower/kubernetes-the-hard-way
- Well-Architected Labs — https://wellarchitectedlabs.com/

## Quick revision checklist (print this)
- [ ] Draw the reference architecture from memory in < 3 min
- [ ] Explain mTLS cert lifecycle (istiod CA, SPIFFE, rotation)
- [ ] PeerAuthentication vs RequestAuthentication vs AuthorizationPolicy
- [ ] RBAC objects + how EKS maps IAM → K8s groups
- [ ] IRSA flow (SA annotation → OIDC → assume role)
- [ ] Kafka partitioning & ordering by VIN; consumer autoscaling on lag
- [ ] Well-Architected: name a decision per pillar
- [ ] DR: state RTO/RPO and the failover steps
- [ ] Two trade-offs you'd volunteer unprompted
- [ ] Three quantified results from the project (STAR)
