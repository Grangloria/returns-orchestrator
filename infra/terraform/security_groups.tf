# -----------------------------------------------------------------------------
# Base ECS Tasks Security Group
# -----------------------------------------------------------------------------
resource "aws_security_group" "ecs_tasks" {
  name        = "returns-ecs-tasks-sg-${var.environment}"
  description = "Allow inbound traffic strictly from ALB"
  vpc_id      = aws_vpc.main.id

  egress {
    description      = "Allow all outbound traffic"
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "returns-ecs-tasks-sg"
  }
}

# -----------------------------------------------------------------------------
# Standalone Ingress Rules: Intra-Cluster Infrastructure
# -----------------------------------------------------------------------------
resource "aws_security_group_rule" "ecs_internal_sqlserver" {
  type                     = "ingress"
  from_port                = 1433
  to_port                  = 1433
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  security_group_id        = aws_security_group.ecs_tasks.id
  description              = "Allow intra-cluster SQL Server communication"
}

resource "aws_security_group_rule" "ecs_internal_redis" {
  type                     = "ingress"
  from_port                = 6379
  to_port                  = 6379
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  security_group_id        = aws_security_group.ecs_tasks.id
  description              = "Allow intra-cluster Redis communication"
}

resource "aws_security_group_rule" "ecs_internal_kafka" {
  type                     = "ingress"
  from_port                = 9092
  to_port                  = 9092
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  security_group_id        = aws_security_group.ecs_tasks.id
  description              = "Allow intra-cluster Kafka communication"
}

# -----------------------------------------------------------------------------
# Standalone Ingress Rules: Microservices (8000-8080)
# -----------------------------------------------------------------------------
resource "aws_security_group_rule" "ecs_internal_microservices" {
  type                     = "ingress"
  from_port                = 8000
  to_port                  = 8080
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  security_group_id        = aws_security_group.ecs_tasks.id
  description              = "Allow intra-cluster communication across all microservice ports (8000-8080)"
}

# -----------------------------------------------------------------------------
# Standalone Ingress Rules: Inbound Traffic from ALB
# -----------------------------------------------------------------------------
resource "aws_security_group_rule" "ecs_from_alb_portal" {
  type                     = "ingress"
  from_port                = 80
  to_port                  = 80
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb.id
  security_group_id        = aws_security_group.ecs_tasks.id
  description              = "Allow ALB traffic to customer portal on port 80"
}

resource "aws_security_group_rule" "ecs_from_alb_gateway" {
  type                     = "ingress"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb.id
  security_group_id        = aws_security_group.ecs_tasks.id
  description              = "Allow ALB traffic to API gateway on port 8080"
}