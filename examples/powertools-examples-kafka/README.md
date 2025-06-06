# Powertools for AWS Lambda (Java) - Kafka Example

This project demonstrates how to use Powertools for AWS Lambda (Java) to deserialize Kafka Lambda events directly into strongly typed Kafka ConsumerRecords<K, V> using different serialization formats.

## Overview

The example showcases automatic deserialization of Kafka Lambda events into ConsumerRecords using three formats:
- JSON - Using standard JSON serialization
- Avro - Using Apache Avro schema-based serialization
- Protobuf - Using Google Protocol Buffers serialization

Each format has its own Lambda function handler that demonstrates how to use the `@Deserialization` annotation with the appropriate `DeserializationType`, eliminating the need to handle complex deserialization logic manually.

## Build and Deploy

### Prerequisites
- [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
- Java 11+
- Maven

### Build

```bash
# Build the application
sam build
```

### Deploy

```bash
# Deploy the application to AWS
sam deploy --guided
```

During the guided deployment, you'll be prompted to provide values for required parameters. After deployment, SAM will output the ARNs of the deployed Lambda functions.

### Build with Different Serialization Formats

The project includes Maven profiles to build with different serialization formats:

```bash
# Build with JSON only (no Avro or Protobuf)
mvn clean package -P base

# Build with Avro only
mvn clean package -P avro-only

# Build with Protobuf only
mvn clean package -P protobuf-only

# Build with all formats (default)
mvn clean package -P full
```

## Testing

The `events` directory contains sample events for each serialization format:
- `kafka-json-event.json` - Sample event with JSON-serialized products
- `kafka-avro-event.json` - Sample event with Avro-serialized products
- `kafka-protobuf-event.json` - Sample event with Protobuf-serialized products

You can use these events to test the Lambda functions:

```bash
# Test the JSON deserialization function
sam local invoke JsonDeserializationFunction --event events/kafka-json-event.json

# Test the Avro deserialization function
sam local invoke AvroDeserializationFunction --event events/kafka-avro-event.json

# Test the Protobuf deserialization function
sam local invoke ProtobufDeserializationFunction --event events/kafka-protobuf-event.json
```

## Sample Generator Tool

The project includes a tool to generate sample JSON, Avro, and Protobuf serialized data. See the [tools/README.md](tools/README.md) for more information.