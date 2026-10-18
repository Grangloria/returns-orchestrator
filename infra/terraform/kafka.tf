# CloudWatch Log Group for Kafka
resource "aws_cloudwatch_log_group" "kafka" {
  name              = "/ecs/returns-kafka-prod"
  retention_in_days = 7
}

# Kafka Task Definition (0.5 vCPU / 1 GB RAM)
resource "aws_ecs_task_definition" "kafka" {
  family                   = "returns-kafka-prod"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name      = "kafka"
      image     = "apache/kafka:latest"
      essential = true

      portMappings = [
        {
          containerPort = 9092
          hostPort      = 9092
          protocol      = "tcp"
        },
        {
          containerPort = 9093
          hostPort      = 9093
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "KAFKA_NODE_ID", value = "1" },
        { name = "KAFKA_PROCESS_ROLES", value = "broker,controller" },
        { name = "KAFKA_LISTENERS", value = "PLAINTEXT://:9092,CONTROLLER://:9093" },
        { name = "KAFKA_ADVERTISED_LISTENERS", value = "PLAINTEXT://kafka.returns-orchestrator-cluster-prod.local:9092" },
        { name = "KAFKA_CONTROLLER_LISTENER_NAMES", value = "CONTROLLER" },
        { name = "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", value = "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT" },
        { name = "KAFKA_CONTROLLER_QUORUM_VOTERS", value = "1@localhost:9093" },
        { name = "KAFKA_AUTO_CREATE_TOPICS_ENABLE", value = "true" },
        # Single-node cluster settings to prevent offset partition stalls:
        { name = "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", value = "1" },
        { name = "KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", value = "1" },
        { name = "KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", value = "1" },
        { name = "KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", value = "0" },
        { name = "CLUSTER_ID", value = "4L622SOARS-IK_v31omWAQ" }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.kafka.name
          "awslogs-region"        = "us-east-2"
          "awslogs-stream-prefix" = "kafka"
        }
      }
    }
  ])
}

# Kafka ECS Fargate Service
resource "aws_ecs_service" "kafka" {
  name            = "kafka"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.kafka.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.kafka.arn
  }

  depends_on = [
    aws_security_group_rule.ecs_internal_kafka
  ]
}