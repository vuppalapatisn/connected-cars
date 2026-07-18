# ---------------------------------------------------------------------------
# EKS cluster + managed node group (Graviton). IRSA enabled so pods assume
# scoped IAM roles via ServiceAccount annotations.
# ---------------------------------------------------------------------------
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.24"

  cluster_name    = var.cluster_name
  cluster_version = var.kubernetes_version

  cluster_endpoint_public_access = true
  enable_irsa                    = true

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  eks_managed_node_groups = {
    default = {
      instance_types = var.node_instance_types
      ami_type       = "AL2_ARM_64"
      desired_size   = var.node_desired_size
      min_size       = var.node_desired_size
      max_size       = var.node_max_size
      labels         = { workload = "general" }
    }
  }

  # Grant the operators group cluster access mapped to a read-only K8s group.
  access_entries = {
    operators = {
      principal_arn = "arn:aws:iam::111122223333:role/connected-cars-operators"
      type          = "STANDARD"
      kubernetes_groups = ["connected-cars-operators"]
    }
  }

  tags = { Cluster = var.cluster_name }
}

# IRSA role for the auth-service to read its JWT signing secret from Secrets Manager.
module "auth_service_irsa" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.44"

  role_name = "connected-cars-auth-service"
  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["connected-cars:auth-service"]
    }
  }
  role_policy_arns = {
    secrets = aws_iam_policy.secrets_read.arn
  }
}

resource "aws_iam_policy" "secrets_read" {
  name = "connected-cars-secrets-read"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = "arn:aws:secretsmanager:${var.region}:*:secret:connected-cars/*"
    }]
  })
}
