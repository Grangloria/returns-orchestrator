# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "returns-orchestrator-cluster-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "portal_logs" {
  name              = "/ecs/returns-customer-portal-${var.environment}"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "gateway_logs" {
  name              = "/ecs/returns-api-gateway-${var.environment}"
  retention_in_days = 7
}

# IAM Role: Task Execution Role (Pulls images from ECR, pushes logs to CloudWatch)
resource "aws_iam_role" "ecs_execution_role" {
  name = "returns-ecs-execution-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution_policy" {
  role       = aws_iam_role.ecs_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# IAM Role: Task Role (Permissions required inside running backend containers)
resource "aws_iam_role" "ecs_task_role" {
  name = "returns-ecs-task-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

# Task Role Policy: S3 Presigned URL generation & DynamoDB persistence
resource "aws_iam_role_policy" "ecs_task_permissions" {
  name = "returns-task-permissions-${var.environment}"
  role = aws_iam_role.ecs_task_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject"
        ]
        Resource = "${aws_s3_bucket.evidence_photos.arn}/*"
      },
      {
        Effect = "Allow"
        Action = [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:Query"
        ]
        Resource = aws_dynamodb_table.returns_events.arn
      }
    ]
  })
}

# Task Definition: Customer Portal
resource "aws_ecs_task_definition" "customer_portal" {
  family                   = "customer-portal-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([{
    name      = "customer-portal"
    image     = "nginx:alpine" # Will be overridden by CI/CD pipeline ECR URI
    essential = true
    portMappings = [{
      containerPort = 80
      hostPort      = 80
    }]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.portal_logs.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "portal"
      }
    }
  }])
}

# Task Definition: API Gateway
resource "aws_ecs_task_definition" "api_gateway" {
  family                   = "api-gateway-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([{
    name      = "api-gateway"
    image     = "openjdk:21-slim" # Will be overridden by CI/CD pipeline ECR URI
    essential = true
    portMappings = [{
      containerPort = 8080
      hostPort      = 8080
    }]
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "AWS_REGION", value = var.aws_region },
      { name = "DYNAMODB_TABLE_NAME", value = aws_dynamodb_table.returns_events.name },
      { name = "S3_BUCKET_NAME", value = aws_s3_bucket.evidence_photos.id }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.gateway_logs.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "gateway"
      }
    }
  }])
}

# ECS Service: Customer Portal (Runs in Private Subnets)
resource "aws_ecs_service" "customer_portal" {
  name            = "customer-portal-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.customer_portal.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.customer_portal.arn
    container_name   = "customer-portal"
    container_port   = 80
  }

  depends_on = [aws_lb_listener.http]
}

# ECS Service: API Gateway (Runs in Private Subnets)
resource "aws_ecs_service" "api_gateway" {
  name            = "api-gateway-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api_gateway.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api_gateway.arn
    container_name   = "api-gateway"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.http]
}