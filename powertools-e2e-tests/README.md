## End-to-end tests

This module is internal and meant to be used for end-to-end (E2E) testing of Powertools for AWS Lambda (Java).

**Prerequisites**:

- An AWS account is needed as well as a local environment able to reach this account
  ([credentials](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html)).
- [Java 11+](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html)
- [Docker](https://docs.docker.com/engine/install/) (or [Finch](https://github.com/runfinch/finch) — if using Finch, set `export CDK_DOCKER=finch` before running tests)

To execute the E2E tests, use the following command: `export JAVA_VERSION=11 && mvn clean verify -Pe2e`

### Under the hood

This module leverages the following components:

- AWS CDK to define the infrastructure and synthesize a CloudFormation template and the assets (lambda function packages)
- The AWS S3 SDK to push the assets on S3
- The AWS CloudFormation SDK to deploy the template
