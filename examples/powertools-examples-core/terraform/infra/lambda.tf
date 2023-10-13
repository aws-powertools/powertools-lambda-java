resource "aws_lambda_function" "hello_world_lambda" {
  runtime           = "java11"
  filename          = "target/helloworld-lambda.jar"
  source_code_hash  = filebase64sha256("target/helloworld-lambda.jar")
  function_name     = "hello_world_lambda"

  handler           = "helloworld.App"
  description       = "Powertools example, deployed by Terraform"
  timeout           = 20
  memory_size       = 512
  role              = "${aws_iam_role.iam_role_for_lambda.arn}"
<<<<<<< HEAD
=======
  environment {
    variables = {
      POWERTOOLS_LOG_LEVEL = "INFO"
      POWERTOOLS_LOGGER_SAMPLE_RATE = "0.1"
      POWERTOOLS_LOGGER_LOG_EVENT = "true"
      POWERTOOLS_METRICS_NAMESPACE = "Coreutilities"
    }
  }
>>>>>>> f6f13a6e66305f05ae92c74bbb308360c2408dd0
  tracing_config {
    mode            = "Active"
  }
  depends_on        = [aws_cloudwatch_log_group.log_group]
}

resource "aws_lambda_function" "hello_world_stream_lambda" {
  runtime           = "java11"
  filename          = "target/helloworld-lambda.jar"
  source_code_hash  = filebase64sha256("target/helloworld-lambda.jar")
  function_name     = "hello_world_stream_lambda"

  handler           = "helloworld.AppStream"
  description       = "Powertools example, deployed by Terraform"
  timeout           = 20
  memory_size       = 512
  role              = "${aws_iam_role.iam_role_for_lambda.arn}"
<<<<<<< HEAD
=======
  environment {
    variables = {
      POWERTOOLS_LOG_LEVEL = "INFO"
      POWERTOOLS_LOGGER_SAMPLE_RATE = "0.7"
      POWERTOOLS_LOGGER_LOG_EVENT = "true"
      POWERTOOLS_METRICS_NAMESPACE = "Coreutilities"
      POWERTOOLS_SERVICE_NAME = "hello"
    }
  }
>>>>>>> f6f13a6e66305f05ae92c74bbb308360c2408dd0
  tracing_config {
    mode            = "Active"
  }
  depends_on        = [aws_cloudwatch_log_group.log_group]
}

# Create a log group for the lambda
resource "aws_cloudwatch_log_group" "log_group" {
  name = "/aws/lambda/hello_world_lambda"
}

# Create a log group for the lambda
resource "aws_cloudwatch_log_group" "log_group_stream" {
  name = "/aws/lambda/hello_world_stream_lambda"
}

# lambda role
resource "aws_iam_role" "iam_role_for_lambda" {
  name = "lambda-invoke-role"
  assume_role_policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Action": "sts:AssumeRole",
        "Principal": {
          "Service": "lambda.amazonaws.com"
        },
        "Effect": "Allow",
        "Sid": ""
      }
    ]
  }
EOF
}

# lambda policy, allow logs to be published to CloudWatch, and traces to Xray
resource "aws_iam_policy" "iam_policy_for_lambda" {
  name = "lambda-invoke-policy"
  path = "/"

  policy = <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "LambdaPolicy",
        "Effect": "Allow",
        "Action": [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "xray:PutTelemetryRecords",
          "xray:PutTraceSegments"
        ],
        "Resource": "*"
      }
    ]
  }
EOF
}

# Attach the policy to the role
resource "aws_iam_role_policy_attachment" "aws_iam_role_policy_attachment" {
  role       = "${aws_iam_role.iam_role_for_lambda.name}"
  policy_arn = "${aws_iam_policy.iam_policy_for_lambda.arn}"
}
