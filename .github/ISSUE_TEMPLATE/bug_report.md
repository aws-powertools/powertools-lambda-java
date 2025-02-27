name: Bug report
description: Report a reproducible bug to help us improve
title: "Bug: TITLE"
labels: ["bug", "triage"]
body:
  - type: markdown
    attributes:
      value: |
        Thank you for submitting a bug report. Please add as much information as possible to help us reproduce, and remove any potential sensitive data.

        Please become familiar with [our definition of bug](https://docs.powertools.aws.dev/lambda/java/processes/maintainers/#is-that-a-bug).
  - type: textarea
    id: expected_behaviour
    attributes:
      label: Expected Behaviour
      description: Please share details on the behaviour you expected
    validations:
      required: true
  - type: textarea
    id: current_behaviour
    attributes:
      label: Current Behaviour
      description: Please share details on the current issue
    validations:
      required: true
  - type: textarea
    id: code_snippet
    attributes:
      label: Code snippet
      description: Please share a code snippet to help us reproduce the issue
      render: java
    validations:
      required: true
  - type: textarea
    id: solution
    attributes:
      label: Possible Solution
      description: If known, please suggest a potential resolution
    validations:
      required: false
  - type: textarea
    id: steps
    attributes:
      label: Steps to Reproduce
      description: Please share how we might be able to reproduce this issue
    validations:
      required: true
  - type: input
    id: version
    attributes:
      label: Powertools for AWS Lambda (Java) version
      placeholder: "latest, 1.19.0"
      value: latest
    validations:
      required: true
  - type: dropdown
    id: runtime
    attributes:
      label: AWS Lambda function runtime
      options:
        - "Java 8"
        - "Java 11"
        - "Java 17"
        - "Java 21"
        - "provided.al2023"
    validations:
      required: true
  - type: dropdown
    id: packaging
    attributes:
      label: Packaging format used
      options:
        - Lambda Layers
        - Serverless Application Repository (SAR) App
        - PyPi
      multiple: true
    validations:
      required: true
  - type: textarea
    id: logs
    attributes:
      label: Debugging logs
      description: If available, please share [debugging logs](https://docs.powertools.aws.dev/lambda/lambda/#debug-mode)
      render: java
    validations:
      required: false
  - type: markdown
    attributes:
      value: |
        ---

        **Disclaimer**: We value your time and bandwidth. As such, any pull requests created on non-triaged issues might not be successful.