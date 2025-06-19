# Kafka Sample Generator Tool

This tool generates base64-encoded serialized products for testing the Kafka consumer functions with different serialization formats.

## Supported Formats

- **JSON**: Generates base64-encoded JSON serialized products
- **Avro**: Generates base64-encoded Avro serialized products
- **Protobuf**: Generates base64-encoded Protobuf serialized products

## Usage

Run the following Maven commands from this directory:

```bash
# Generate Avro and Protobuf classes from schemas
mvn generate-sources

# Compile the code
mvn compile
```

### Generate JSON Samples

```bash
# Run the JSON sample generator
mvn exec:java -Dexec.mainClass="org.demo.kafka.tools.GenerateJsonSamples"
```

The tool will output base64-encoded values for JSON products that can be used in `../events/kafka-json-event.json`.

### Generate Avro Samples

```bash
# Run the Avro sample generator
mvn exec:java -Dexec.mainClass="org.demo.kafka.tools.GenerateAvroSamples"
```

The tool will output base64-encoded values for Avro products that can be used in `../events/kafka-avro-event.json`.

### Generate Protobuf Samples

```bash
# Run the Protobuf sample generator
mvn exec:java -Dexec.mainClass="org.demo.kafka.tools.GenerateProtobufSamples"
```

The tool will output base64-encoded values for Protobuf products that can be used in `../events/kafka-protobuf-event.json`. This generator creates samples with and without Confluent message-indexes to test different serialization scenarios.

## Output

Each generator produces:

1. Three different products (Laptop, Smartphone, Headphones)
2. An integer key (42) and one entry with a nullish key to test for edge-cases
3. A complete sample event structure that can be used directly for testing

The Protobuf generators additionally create samples with different Confluent message-index formats:
- Standard protobuf (no message indexes)
- Simple message index (single 0 byte)
- Complex message index (length-prefixed array)

For more information about Confluent Schema Registry serialization formats and wire format specifications, see the [Confluent documentation](https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format).

## Example

After generating the samples, you can copy the output into the respective event files:

- `../events/kafka-json-event.json` for JSON samples
- `../events/kafka-avro-event.json` for Avro samples
- `../events/kafka-protobuf-event.json` for Protobuf samples

These event files can then be used to test the Lambda functions with the appropriate deserializer.
