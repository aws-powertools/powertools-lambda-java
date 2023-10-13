# terraform modules
module "powertools_for_java_lambda" {
  source = "./infra/"
}
<<<<<<< HEAD

output "api_url" {
  value       = module.powertools_for_java_lambda.invoke
  description = "URL where the API gateway can be invoked"
}
=======
>>>>>>> f6f13a6e66305f05ae92c74bbb308360c2408dd0
