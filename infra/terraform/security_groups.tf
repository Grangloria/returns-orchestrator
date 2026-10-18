# Allow internal traffic on port 9092 (Kafka)
resource "aws_security_group_rule" "ecs_internal_kafka" {
  type                     = "ingress"
  from_port                = 9092
  to_port                  = 9092
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  security_group_id        = aws_security_group.ecs_tasks.id
  description              = "Allow intra-cluster Kafka communication"
}

# Allow internal traffic on port 6379 (Redis)
resource "aws_security_group_rule" "ecs_internal_redis" {
  type                     = "ingress"
  from_port                = 6379
  to_port                  = 6379
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  security_group_id        = aws_security_group.ecs_tasks.id
  description              = "Allow intra-cluster Redis communication"
}

# Allow internal traffic on port 1433 (SQL Server)
resource "aws_security_group_rule" "ecs_internal_sqlserver" {
  type                     = "ingress"
  from_port                = 1433
  to_port                  = 1433
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  security_group_id        = aws_security_group.ecs_tasks.id
  description              = "Allow intra-cluster SQL Server communication"
}

# Allow internal traffic on port 8000 (Returns Service)
resource "aws_security_group_rule" "ecs_internal_returns_service" {
  type                     = "ingress"
  from_port                = 8000
  to_port                  = 8000
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  security_group_id        = aws_security_group.ecs_tasks.id
  description              = "Allow intra-cluster communication to returns-service on port 8000"
}

# Allow all microservices (8000-8080) to communicate internally
resource "aws_security_group_rule" "ecs_internal_microservices" {
  type                     = "ingress"
  from_port                = 8000
  to_port                  = 8080
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  security_group_id        = aws_security_group.ecs_tasks.id
  description              = "Allow intra-cluster communication across all microservice ports (8000-8080)"
}