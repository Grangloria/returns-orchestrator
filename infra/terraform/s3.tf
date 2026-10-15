resource "random_id" "bucket_suffix" {
  byte_length = 4
}

resource "aws_s3_bucket" "evidence_photos" {
  bucket        = "returns-evidence-photos-${var.environment}-${random_id.bucket_suffix.hex}"
  force_destroy = false
}

resource "aws_s3_bucket_public_access_block" "evidence_photos" {
  bucket = aws_s3_bucket.evidence_photos.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_cors_configuration" "evidence_photos" {
  bucket = aws_s3_bucket.evidence_photos.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["PUT", "POST", "GET", "HEAD"]
    allowed_origins = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}