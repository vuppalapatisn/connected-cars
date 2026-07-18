# ---------------------------------------------------------------------------
# Managed data stores: RDS PostgreSQL (Multi-AZ), Amazon MSK (Kafka),
# ElastiCache Redis. All in private subnets, reachable only from the EKS SG.
# ---------------------------------------------------------------------------

resource "aws_security_group" "data" {
  name_prefix = "${local.name}-data-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "From EKS nodes"
    from_port       = 0
    to_port         = 0
    protocol        = "-1"
    security_groups = [module.eks.node_security_group_id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# ---- RDS PostgreSQL ----
resource "aws_db_subnet_group" "this" {
  name       = "${local.name}-db"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_db_instance" "postgres" {
  identifier             = "${local.name}-postgres"
  engine                 = "postgres"
  engine_version         = "16"
  instance_class         = var.db_instance_class
  allocated_storage      = 100
  storage_encrypted      = true
  db_name                = "connectedcars"
  username               = "connectedcars"
  password               = var.db_password
  multi_az               = var.environment == "prod"
  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.data.id]
  skip_final_snapshot    = var.environment != "prod"
  backup_retention_period = var.environment == "prod" ? 14 : 1
}

# ---- Amazon MSK (Kafka) ----
resource "aws_msk_cluster" "this" {
  cluster_name           = "${local.name}-msk"
  kafka_version          = "3.7.x"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type   = "kafka.m5.large"
    client_subnets  = module.vpc.private_subnets
    security_groups = [aws_security_group.data.id]
    storage_info {
      ebs_storage_info { volume_size = 200 }
    }
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }
}

# ---- ElastiCache Redis ----
resource "aws_elasticache_subnet_group" "this" {
  name       = "${local.name}-redis"
  subnet_ids = module.vpc.private_subnets
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "${local.name}-redis"
  description                = "Connected Cars Redis"
  engine                     = "redis"
  node_type                  = "cache.r6g.large"
  num_cache_clusters         = var.environment == "prod" ? 2 : 1
  automatic_failover_enabled = var.environment == "prod"
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  subnet_group_name          = aws_elasticache_subnet_group.this.name
  security_group_ids         = [aws_security_group.data.id]
}
