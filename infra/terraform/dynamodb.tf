resource "aws_dynamodb_table" "returns_events" {
  name         = "returns-events-${var.environment}"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "returnId"
  range_key    = "timestamp"

  attribute {
    name = "returnId"
    type = "S"
  }

  attribute {
    name = "timestamp"
    type = "N"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = {
    Name = "returns-events-table"
  }
}