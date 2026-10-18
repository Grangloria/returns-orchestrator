# Route 53 Private Service Discovery Namespace
resource "aws_service_discovery_private_dns_namespace" "cluster" {
  name        = "returns-orchestrator-cluster-${var.environment}.local"
  description = "Private DNS namespace for ECS cluster services"
  vpc         = aws_vpc.main.id
}

# Cloud Map Discovery: returns-service
resource "aws_service_discovery_service" "returns_service" {
  name = "returns-service"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.cluster.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }
}

# Cloud Map Discovery: sqlserver
resource "aws_service_discovery_service" "sqlserver" {
  name = "sqlserver"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.cluster.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }
}

# Service Discovery: Apache Kafka
resource "aws_service_discovery_service" "kafka" {
  name = "kafka"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.cluster.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }
}

# Service Discovery: Redis Cache & Locking
resource "aws_service_discovery_service" "redis" {
  name = "redis"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.cluster.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }
}

# Cloud Map Discovery: Downstream Services (Inventory, Notification, Refund, Carrier, Mock API)
resource "aws_service_discovery_service" "downstream" {
  for_each = local.downstream_services

  name = each.key

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.cluster.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }
}