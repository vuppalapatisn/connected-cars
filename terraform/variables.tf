variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name (dev/staging/prod)"
  type        = string
  default     = "dev"
}

variable "cluster_name" {
  description = "EKS cluster name"
  type        = string
  default     = "connected-cars"
}

variable "kubernetes_version" {
  type    = string
  default = "1.30"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "node_instance_types" {
  type    = list(string)
  default = ["m6g.large"] # Graviton (ARM) for cost/perf
}

variable "node_desired_size" {
  type    = number
  default = 3
}

variable "node_max_size" {
  type    = number
  default = 10
}

variable "db_instance_class" {
  type    = string
  default = "db.r6g.large"
}

variable "db_password" {
  description = "Master password for RDS (inject via TF_VAR_db_password / Secrets Manager)"
  type        = string
  sensitive   = true
  default     = ""
}
