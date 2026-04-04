# Tell Terraform we're using AWS
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Configure AWS - region Paris
provider "aws" {
  region = "eu-west-3"
}

# ============================================
# S3 BUCKETS
# ============================================

# Input bucket - where you upload original images
resource "aws_s3_bucket" "input_bucket" {
  bucket = "image-compression-input-${random_id.bucket_suffix.hex}"
}

# Output bucket - where compressed images are saved
resource "aws_s3_bucket" "output_bucket" {
  bucket = "image-compression-output-${random_id.bucket_suffix.hex}"
}

# Random suffix to make bucket names unique globally
resource "random_id" "bucket_suffix" {
  byte_length = 4
}

# ============================================
# SQS QUEUE
# ============================================

# Queue to receive S3 events
resource "aws_sqs_queue" "image_queue" {
  name                       = "image-compression-queue"
  visibility_timeout_seconds = 120  # Should be > Lambda timeout
  message_retention_seconds  = 86400  # 1 day
}

# Policy: Allow S3 to send messages to SQS
resource "aws_sqs_queue_policy" "s3_to_sqs" {
  queue_url = aws_sqs_queue.image_queue.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "s3.amazonaws.com"
      }
      Action   = "sqs:SendMessage"
      Resource = aws_sqs_queue.image_queue.arn
      Condition = {
        ArnEquals = {
          "aws:SourceArn" = aws_s3_bucket.input_bucket.arn
        }
      }
    }]
  })
}

# ============================================
# S3 EVENT TRIGGER
# ============================================

# When image is uploaded to input bucket, send event to SQS
resource "aws_s3_bucket_notification" "input_bucket_notification" {
  bucket = aws_s3_bucket.input_bucket.id

  queue {
    queue_arn     = aws_sqs_queue.image_queue.arn
    events        = ["s3:ObjectCreated:*"]
    filter_suffix = ".png"  # Only trigger for PNG files
  }

  depends_on = [aws_sqs_queue_policy.s3_to_sqs]
}

# ============================================
# LAMBDA SQS TRIGGER
# ============================================

# Lambda reads from SQS queue
resource "aws_lambda_event_source_mapping" "sqs_trigger" {
  event_source_arn = aws_sqs_queue.image_queue.arn
  function_name    = aws_lambda_function.sqs_processor.arn
  batch_size       = 1  # Process one image at a time
  enabled          = false
}

# ============================================
# SNS (EMAIL NOTIFICATIONS)
# ============================================

# Variable for email address
variable "notification_email" {
  description = "Email address for compression notifications"
  type        = string
  default     = "ahmad.abuhannud@gmail.com"  
}

# SNS Topic for notifications
resource "aws_sns_topic" "compression_notifications" {
  name = "image-compression-notifications"
}

# Email subscription
resource "aws_sns_topic_subscription" "email_subscription" {
  topic_arn = aws_sns_topic.compression_notifications.arn
  protocol  = "email"
  endpoint  = var.notification_email
}

# ============================================
# IAM ROLE FOR LAMBDA
# ============================================

# The role that Lambda will assume
resource "aws_iam_role" "lambda_role" {
  name = "image-compression-lambda-role"

  # This policy allows Lambda service to assume this role
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

# Policy: Allow Lambda to read/write S3, SQS, and write logs
resource "aws_iam_role_policy" "lambda_policy" {
  name = "image-compression-lambda-policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        # S3 permissions
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ]
        Resource = [
          "${aws_s3_bucket.input_bucket.arn}/*",
          "${aws_s3_bucket.output_bucket.arn}/*"
        ]
      },
      {
        # SQS permissions
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = aws_sqs_queue.image_queue.arn
      },
      {
        # CloudWatch Logs permissions
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        # SNS permissions (for email notifications)
        Effect = "Allow"
        Action = [
          "sns:Publish"
        ]
        Resource = aws_sns_topic.compression_notifications.arn
      }
    ]
  })
}
# ============================================
# LAMBDA FUNCTIONS
# ============================================

# Lambda 1: API Gateway handler (manual compression via curl)
resource "aws_lambda_function" "compressor" {
  function_name = "image-compression-api"
  role          = aws_iam_role.lambda_role.arn
  handler       = "handler.LambdaHandler::handleRequest"
  runtime       = "java11"
  timeout       = 60
  memory_size   = 512

  filename         = "${path.module}/../../target/image-compression-lambda-1.0.jar"
  source_code_hash = filebase64sha256("${path.module}/../../target/image-compression-lambda-1.0.jar")
}

# Lambda 2: SQS handler (automatic compression on S3 upload)
resource "aws_lambda_function" "sqs_processor" {
  function_name = "image-compression-sqs"
  role          = aws_iam_role.lambda_role.arn
  handler       = "handler.SqsHandler::handleRequest"
  runtime       = "java11"
  timeout       = 60
  memory_size   = 512

  filename         = "${path.module}/../../target/image-compression-lambda-1.0.jar"
  source_code_hash = filebase64sha256("${path.module}/../../target/image-compression-lambda-1.0.jar")

  # Environment variable for SNS topic
  environment {
    variables = {
      SNS_TOPIC_ARN = aws_sns_topic.compression_notifications.arn
    }
  }
}
# ============================================
# API GATEWAY (REST API)
# ============================================

# Create REST API
resource "aws_api_gateway_rest_api" "api" {
  name        = "image-compression-api"
  description = "REST API for image compression"
}

# Create /compress resource
resource "aws_api_gateway_resource" "compress" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = "compress"
}

# Create POST method
resource "aws_api_gateway_method" "post_compress" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.compress.id
  http_method   = "POST"
  authorization = "NONE"
}

# Connect to Lambda
resource "aws_api_gateway_integration" "lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.compress.id
  http_method             = aws_api_gateway_method.post_compress.http_method
  integration_http_method = "POST"
  type                    = "AWS"
  uri                     = aws_lambda_function.compressor.invoke_arn
}

# Method response
resource "aws_api_gateway_method_response" "response_200" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.compress.id
  http_method = aws_api_gateway_method.post_compress.http_method
  status_code = "200"
}

# Integration response
resource "aws_api_gateway_integration_response" "response" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  resource_id = aws_api_gateway_resource.compress.id
  http_method = aws_api_gateway_method.post_compress.http_method
  status_code = aws_api_gateway_method_response.response_200.status_code

  depends_on = [aws_api_gateway_integration.lambda_integration]
}

# Deploy the API
resource "aws_api_gateway_deployment" "deployment" {
  rest_api_id = aws_api_gateway_rest_api.api.id

  depends_on = [
    aws_api_gateway_integration.lambda_integration,
    aws_api_gateway_integration_response.response
  ]

  lifecycle {
    create_before_destroy = true
  }
}

# Create prod stage
resource "aws_api_gateway_stage" "prod" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  deployment_id = aws_api_gateway_deployment.deployment.id
  stage_name    = "prod"
}

# Allow API Gateway to invoke Lambda
resource "aws_lambda_permission" "api_gateway_permission" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.compressor.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/*/*"
}


output "api_url" {
  value = "${aws_api_gateway_stage.prod.invoke_url}/compress"
}

output "input_bucket" {
  value = aws_s3_bucket.input_bucket.bucket
}

output "output_bucket" {
  value = aws_s3_bucket.output_bucket.bucket
}

output "sqs_queue_url" {
  value = aws_sqs_queue.image_queue.url
}

output "sns_topic_arn" {
  value = aws_sns_topic.compression_notifications.arn
}
