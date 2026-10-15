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
