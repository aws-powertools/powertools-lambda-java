#  Powertools for AWS Lambda (Java) - Core Utilities Example with Serverless Framework

This project demonstrates the Lambda for Powertools Java module deployed using [Serverless Framework](https://www.serverless.com/framework).

For general information on the deployed example itself, you can refer to the parent [README](../README.md).
To install Serverless Framework if you don't have it yet, you can follow the [Getting Started Guide](https://www.serverless.com/framework/docs/getting-started)

## Configuration
Serverless Framework uses [serverless.yml](./serverless.yml) to define the application's AWS resources.
This file defines the Lambda function to be deployed as well as API Gateway for it.

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.


## Deploy the sample application

The minimum to deploy the app should be
```bash 
mvn package && sls deploy
```

## Useful commands
Deploy your entire stack
```bash
sls deploy
``` 

Deploy a single function
```bash 
sls deploy function -f hello
```