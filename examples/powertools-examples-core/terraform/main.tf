terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# terraform modules
module "powertools_for_java_lambda" {
  source = "./infra/"
}

output "api_url" {
  value       = module.powertools_for_java_lambda.invoke
  description = "URL where the API gateway can be invoked"
}

# Configure the AWS Provider
provider "aws" {
  region = "us-east-1"
}