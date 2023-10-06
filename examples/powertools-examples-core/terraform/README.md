#  Powertools for AWS Lambda (Java) - Core Utilities Example with Terraform

This project demonstrates the Lambda for Powertools Java module deployed using [Terraform](https://www.terraform.io/).
For general information on the deployed example itself, you can refer to the parent [README](../README.md).
To install Terraform if you don't have it yet, you can follow the [Install Terraform Guide](https://developer.hashicorp.com/terraform/downloads?product_intent=terraform).

## Configuration
Serverless Framework uses [serverless.yml](./serverless.yml) to define the application's AWS resources.
This file defines the Lambda function to be deployed as well as API Gateway for it.

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.


## Deploy the sample application

To deploy the app, simply run the following commands:
```bash 
terraform init
mvn package && terraform apply
```

## Useful commands

To destroy the app
```bash 
terraform destroy
```
