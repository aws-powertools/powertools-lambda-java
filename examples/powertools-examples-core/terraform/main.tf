# terraform modules
module "powertools_for_java_lambda" {
  source = "./infra/"
}

output "api_url" {
  value       = module.powertools_for_java_lambda.invoke
  description = "URL where the API gateway can be invoked"
}