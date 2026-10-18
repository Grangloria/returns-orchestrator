# -----------------------------------------------------------------------------
# ECS Cluster
# -----------------------------------------------------------------------------
resource "aws_ecs_cluster" "main" {
  name = "returns-orchestrator-cluster-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# -----------------------------------------------------------------------------
# CloudWatch Log Groups
# -----------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "portal_logs" {
  name              = "/ecs/returns-customer-portal-${var.environment}"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "gateway_logs" {
  name              = "/ecs/returns-api-gateway-${var.environment}"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "returns_logs" {
  name              = "/ecs/returns-service-${var.environment}"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "sqlserver_logs" {
  name              = "/ecs/returns-sqlserver-${var.environment}"
  retention_in_days = 7
}

# -----------------------------------------------------------------------------
# IAM Roles & Policies
# -----------------------------------------------------------------------------
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

resource "aws_iam_role_policy_attachment" "ecs_task_ssm" {
  role       = aws_iam_role.ecs_task_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# -----------------------------------------------------------------------------
# Customer Portal: Task Definition & Service
# -----------------------------------------------------------------------------
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
    image     = "594116288585.dkr.ecr.us-east-2.amazonaws.com/customer-portal:latest"
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

# -----------------------------------------------------------------------------
# API Gateway: Task Definition & Service
# -----------------------------------------------------------------------------
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
    image     = "594116288585.dkr.ecr.us-east-2.amazonaws.com/api-gateway:latest"
    essential = true
    portMappings = [{
      containerPort = 8080
      hostPort      = 8080
    }]
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "AWS_REGION", value = var.aws_region },
      { name = "DYNAMODB_TABLE_NAME", value = aws_dynamodb_table.returns_events.name },
      { name = "S3_BUCKET_NAME", value = aws_s3_bucket.evidence_photos.id },
      { name = "RETURNS_SERVICE_HOST", value = "returns-service.returns-orchestrator-cluster-${var.environment}.local" },
      { name = "RETURNS_SERVICE_PORT", value = "8000" },
      { name = "REDIS_HOST", value = "redis.returns-orchestrator-cluster-${var.environment}.local" },
      { name = "REDIS_PORT", value = "6379" }
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

# -----------------------------------------------------------------------------
# SQL Server 2022 Express: Task Definition & Service
# -----------------------------------------------------------------------------
resource "aws_ecs_task_definition" "sqlserver" {
  family                   = "returns-sqlserver-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "1024"
  memory                   = "2048"
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([{
    name      = "sqlserver"
    image     = "mcr.microsoft.com/mssql/server:2022-latest"
    essential = true

    environment = [
      { name = "ACCEPT_EULA", value = "Y" },
      { name = "MSSQL_PID", value = "Express" },
      { name = "MSSQL_SA_PASSWORD", value = "ProdPass123!" }
    ]

    entryPoint = ["/bin/bash", "-c"]
    command = [
      <<-EOT
      /opt/mssql/bin/sqlservr & pid=$!;
      for i in {1..30}; do
        /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'ProdPass123!' -C -Q 'SELECT 1' && break;
        sleep 2;
      done;
      /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'ProdPass123!' -C -Q "IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'returns_db') CREATE DATABASE returns_db;";
      /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'ProdPass123!' -d returns_db -C -Q "
      IF OBJECT_ID('return_manifests', 'U') IS NULL
      CREATE TABLE return_manifests (
          id BIGINT IDENTITY(1,1) PRIMARY KEY,
          order_id VARCHAR(50) NOT NULL,
          sku VARCHAR(100) NOT NULL,
          item_name VARCHAR(255),
          quantity INT,
          customer_email VARCHAR(255),
          zip_code VARCHAR(20),
          reason VARCHAR(500),
          status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
          label_url VARCHAR(500),
          created_at DATETIME2 NOT NULL DEFAULT GETDATE()
      );
      IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_manifests_order_id')
      CREATE INDEX idx_manifests_order_id ON return_manifests(order_id);

      IF OBJECT_ID('outbox', 'U') IS NULL
      CREATE TABLE outbox (
          id BIGINT IDENTITY(1,1) PRIMARY KEY,
          aggregate_type VARCHAR(100) NOT NULL,
          aggregate_id VARCHAR(100) NOT NULL,
          event_type VARCHAR(100) NOT NULL,
          payload NVARCHAR(MAX) NOT NULL,
          processed BIT DEFAULT 0 NOT NULL,
          created_at DATETIME2 DEFAULT SYSDATETIME()
      );
      IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_outbox_processed')
      CREATE INDEX idx_outbox_processed ON outbox(processed);
      ";
      wait $pid
      EOT
    ]

    portMappings = [{
      containerPort = 1433
      hostPort      = 1433
      protocol      = "tcp"
    }]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.sqlserver_logs.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "sqlserver"
      }
    }
  }])
}

resource "aws_ecs_service" "sqlserver" {
  name                   = "sqlserver-service"
  cluster                = aws_ecs_cluster.main.id
  task_definition        = aws_ecs_task_definition.sqlserver.arn
  desired_count          = 1
  launch_type            = "FARGATE"
  enable_execute_command = true

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.sqlserver.arn
  }
}

# -----------------------------------------------------------------------------
# Returns Service: Task Definition & Service
# -----------------------------------------------------------------------------
resource "aws_ecs_task_definition" "returns_service" {
  family                   = "returns-service-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([{
    name      = "returns-service"
    image     = "594116288585.dkr.ecr.us-east-2.amazonaws.com/returns-service:latest"
    essential = true

    portMappings = [{
      containerPort = 8000
      hostPort      = 8000
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "AWS_REGION", value = var.aws_region },
      { name = "S3_BUCKET_NAME", value = aws_s3_bucket.evidence_photos.id },
      { name = "DYNAMODB_TABLE_NAME", value = aws_dynamodb_table.returns_events.name },
      { name = "DB_HOST", value = "sqlserver.returns-orchestrator-cluster-${var.environment}.local" },
      { name = "DB_PORT", value = "1433" },
      { name = "DB_PASS", value = "ProdPass123!" },
      { name = "SPRING_FLYWAY_ENABLED", value = "false" },
      { name = "SPRING_KAFKA_BOOTSTRAP_SERVERS", value = "kafka.returns-orchestrator-cluster-${var.environment}.local:9092" },
      { name = "SPRING_KAFKA_LISTENER_AUTO_STARTUP", value = "true" },
      { name = "SPRING_DATA_REDIS_HOST", value = "redis.returns-orchestrator-cluster-${var.environment}.local" },
      { name = "SPRING_DATA_REDIS_PORT", value = "6379" },
      { name = "APP_OUTBOX_ENABLED", value = "true" }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.returns_logs.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "returns"
      }
    }
  }])
}

resource "aws_ecs_service" "returns_service" {
  name            = "returns-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.returns_service.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.returns_service.arn
  }
}

# -----------------------------------------------------------------------------
# Downstream Services: Log Groups
# -----------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "inventory_logs" { name = "/ecs/inventory-service-${var.environment}" }
resource "aws_cloudwatch_log_group" "notification_logs" { name = "/ecs/notification-service-${var.environment}" }
resource "aws_cloudwatch_log_group" "refund_logs" { name = "/ecs/refund-service-${var.environment}" }
resource "aws_cloudwatch_log_group" "carrier_gateway_logs" { name = "/ecs/carrier-gateway-${var.environment}" }
resource "aws_cloudwatch_log_group" "mock_carrier_logs" { name = "/ecs/mock-external-carrier-api-${var.environment}" }

# -----------------------------------------------------------------------------
# Downstream Services: Local Configuration Locals
# -----------------------------------------------------------------------------
locals {
  downstream_services = {
    "inventory-service"         = { port = 8001, log_group = aws_cloudwatch_log_group.inventory_logs.name }
    "notification-service"      = { port = 8002, log_group = aws_cloudwatch_log_group.notification_logs.name }
    "mock-external-carrier-api" = { port = 8003, log_group = aws_cloudwatch_log_group.mock_carrier_logs.name }
    "carrier-gateway"           = { port = 8004, log_group = aws_cloudwatch_log_group.carrier_gateway_logs.name }
    "refund-service"            = { port = 8006, log_group = aws_cloudwatch_log_group.refund_logs.name }
  }
}

# -----------------------------------------------------------------------------
# Downstream Services: Task Definitions & ECS Services
# -----------------------------------------------------------------------------
resource "aws_ecs_task_definition" "downstream_tasks" {
  for_each                 = local.downstream_services
  family                   = "${each.key}-${var.environment}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([{
    name      = each.key
    image     = "594116288585.dkr.ecr.us-east-2.amazonaws.com/${each.key}:latest"
    essential = true
    portMappings = [{
      containerPort = each.value.port
      hostPort      = each.value.port
      protocol      = "tcp"
    }]
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "NODE_ENV", value = "production" },
      { name = "AWS_REGION", value = var.aws_region },

      # Kafka & Redis wiring
      { name = "SPRING_KAFKA_BOOTSTRAP_SERVERS", value = "kafka.returns-orchestrator-cluster-${var.environment}.local:9092" },
      { name = "KAFKA_BROKERS", value = "kafka.returns-orchestrator-cluster-${var.environment}.local:9092" },
      { name = "SPRING_DATA_REDIS_HOST", value = "redis.returns-orchestrator-cluster-${var.environment}.local" },
      { name = "SPRING_DATA_REDIS_PORT", value = "6379" },

      # Service Discovery wiring for carrier-gateway to mock carrier API
      { name = "MOCK_CARRIER_API_URL", value = "http://mock-external-carrier-api.returns-orchestrator-cluster-${var.environment}.local:8003" },
      { name = "CARRIER_API_URL", value = "http://mock-external-carrier-api.returns-orchestrator-cluster-${var.environment}.local:8003" },
      { name = "CARRIER_SERVICE_URL", value = "http://mock-external-carrier-api.returns-orchestrator-cluster-${var.environment}.local:8003" },
      { name = "CARRIER_CLIENT_BASE_URL", value = "http://mock-external-carrier-api.returns-orchestrator-cluster-${var.environment}.local:8003" },

      # Database wiring (R2DBC for the App)
      { name = "SPRING_R2DBC_URL", value = "r2dbc:mssql://sqlserver.returns-orchestrator-cluster-${var.environment}.local:1433/returns_db" },
      { name = "SPRING_R2DBC_USERNAME", value = "sa" },
      { name = "SPRING_R2DBC_PASSWORD", value = "ProdPass123!" },

      # Database wiring (JDBC for Flyway Migrations)
      { name = "SPRING_FLYWAY_URL", value = "jdbc:sqlserver://sqlserver.returns-orchestrator-cluster-${var.environment}.local:1433;databaseName=returns_db;encrypt=true;trustServerCertificate=true;" },
      { name = "SPRING_FLYWAY_USER", value = "sa" },
      { name = "SPRING_FLYWAY_PASSWORD", value = "ProdPass123!" },

      # Google Workspace SMTP Configuration
      { name = "SPRING_MAIL_HOST", value = "smtp.gmail.com" },
      { name = "SPRING_MAIL_PORT", value = "587" },
      { name = "SPRING_MAIL_USERNAME", value = "james.hayes@granglorialabs.com" },
      { name = "SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH", value = "true" },
      { name = "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE", value = "true" },
      { name = "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED", value = "true" },
    ]

    # Injects Google App Password from SSM only for notification-service
    secrets = each.key == "notification-service" ? [
      {
        name      = "SPRING_MAIL_PASSWORD"
        valueFrom = "arn:aws:ssm:us-east-2:594116288585:parameter/returns/prod/GOOGLE_MAIL_APP_PASSWORD"
      }
    ] : []

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = each.value.log_group
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "app"
      }
    }
  }])
}

resource "aws_ecs_service" "downstream_services" {
  for_each        = local.downstream_services
  name            = "${each.key}-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.downstream_tasks[each.key].arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.downstream[each.key].arn
  }
}

resource "aws_iam_role_policy" "ecs_execution_ssm" {
  name = "returns-execution-ssm-${var.environment}"
  role = aws_iam_role.ecs_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssm:GetParameters",
          "kms:Decrypt"
        ]
        Resource = "*"
      }
    ]
  })
}