terraform {
  backend "s3" {
    # Replace <YOUR_ACCOUNT_ID> with the output from Step 1
    bucket         = "returns-orchestrator-tfstate-prod-594116288585"
    key            = "prod/terraform.tfstate"
    region         = "us-east-2"
    encrypt        = true
    dynamodb_table = "returns-orchestrator-tflocks-prod"
  }
}