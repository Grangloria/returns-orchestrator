output "alb_dns_name" {
  description = "Public URL for accessing the application"
  value       = aws_lb.main.dns_name
}

output "s3_evidence_bucket" {
  description = "S3 Bucket Name for Return Evidence Photos"
  value       = aws_s3_bucket.evidence_photos.id
}

output "dynamodb_table_name" {
  description = "DynamoDB Events Table"
  value       = aws_dynamodb_table.returns_events.name
}

output "ecs_cluster_name" {
  description = "ECS Cluster Name"
  value       = aws_ecs_cluster.main.name
}