resource "aws_api_gateway_rest_api" "hello_world_api" {
  name        = "hello_world_api"
  description = "API Gateway endpoint URL for Prod stage for Hello World function"
}

resource "aws_api_gateway_resource" "hello_resource" {
  rest_api_id = "${aws_api_gateway_rest_api.hello_world_api.id}"
  parent_id   = "${aws_api_gateway_rest_api.hello_world_api.root_resource_id}"
  path_part   = "hello"
}

resource "aws_api_gateway_resource" "hello_stream_resource" {
  rest_api_id = "${aws_api_gateway_rest_api.hello_world_api.id}"
  parent_id   = "${aws_api_gateway_rest_api.hello_world_api.root_resource_id}"
  path_part   = "hellostream"
}

resource "aws_api_gateway_method" "hello_get_method" {
  rest_api_id   = "${aws_api_gateway_rest_api.hello_world_api.id}"
  resource_id   = "${aws_api_gateway_resource.hello_resource.id}"
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "hello_stream_get_method" {
  rest_api_id   = "${aws_api_gateway_rest_api.hello_world_api.id}"
  resource_id   = "${aws_api_gateway_resource.hello_stream_resource.id}"
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "java_lambda_integration" {
  rest_api_id             = "${aws_api_gateway_rest_api.hello_world_api.id}"
  resource_id             = "${aws_api_gateway_resource.hello_resource.id}"
  http_method             = "${aws_api_gateway_method.hello_get_method.http_method}"

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "${aws_lambda_function.hello_world_lambda.invoke_arn}"
}

resource "aws_api_gateway_integration" "java_stream_lambda_integration" {
  rest_api_id             = "${aws_api_gateway_rest_api.hello_world_api.id}"
  resource_id             = "${aws_api_gateway_resource.hello_stream_resource.id}"
  http_method             = "${aws_api_gateway_method.hello_stream_get_method.http_method}"

  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "${aws_lambda_function.hello_world_stream_lambda.invoke_arn}"
}

resource "aws_api_gateway_deployment" "prod_deployment" {
  depends_on  = [aws_api_gateway_integration.java_lambda_integration, aws_api_gateway_integration.java_stream_lambda_integration]
  rest_api_id = "${aws_api_gateway_rest_api.hello_world_api.id}"
  stage_name  = "prod"
}

# Allows API gateway to invoke lambda
resource "aws_lambda_permission" "hello_world_lambda_invoke" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.hello_world_lambda.function_name}"
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.hello_world_api.execution_arn}/${aws_api_gateway_deployment.prod_deployment.stage_name}/GET/hello"
}

# Allows API gateway to invoke lambda
resource "aws_lambda_permission" "hello_world_lambda_testinvoke" {
  statement_id  = "AllowAPIGatewayTestInvoke"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.hello_world_lambda.function_name}"
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.hello_world_api.execution_arn}/test-invoke-stage/GET/hello"
}

# Allows API gateway to invoke lambda
resource "aws_lambda_permission" "hello_world_stream_lambda_invoke" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.hello_world_stream_lambda.function_name}"
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.hello_world_api.execution_arn}/${aws_api_gateway_deployment.prod_deployment.stage_name}/GET/hellostream"
}

# Allows API gateway to invoke lambda
resource "aws_lambda_permission" "hello_world_stream_lambda_testinvoke" {
  statement_id  = "AllowAPIGatewayTestInvoke"
  action        = "lambda:InvokeFunction"
  function_name = "${aws_lambda_function.hello_world_stream_lambda.function_name}"
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.hello_world_api.execution_arn}/test-invoke-stage/GET/hellostream"
}
