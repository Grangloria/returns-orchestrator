terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Uncomment once your S3 remote state bucket and DynamoDB lock table are created:
  # backend "s3" {
  #   bucket         = "returns-orchestrator-tf-state"
  #   key            = "prod/terraform.tfstate"
  #   region         = "us-east-1"
  #   dynamodb_table = "returns-orchestrator-tf-locks"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "returns-orchestrator"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}