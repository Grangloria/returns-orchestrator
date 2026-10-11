#!/bin/bash
echo "[LOCALSTACK] Initializing S3 Bucket..."

awslocal s3 mb s3://grangloria-return-photos

awslocal s3api put-bucket-cors \
  --bucket grangloria-return-photos \
  --cors-configuration file:///etc/localstack/init/ready.d/cors.json

echo "[LOCALSTACK] S3 Bucket 'grangloria-return-photos' ready with CORS rules."