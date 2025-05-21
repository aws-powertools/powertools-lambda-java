# Avro Sample Generator Tool

This tool generates base64-encoded Avro serialized products for testing the Kafka Avro consumer function.

## Usage

Run the following Maven commands from this directory:

```bash
# Generate Avro classes from schema
mvn generate-sources

# Compile the code
mvn compile

# Run the tool
mvn exec:java
```

The tool will output base64-encoded values for three different Avro products and an integer key.
You can copy these values into the `../events/kafka-avro-event.json` file to create a test event.

## Output

The tool generates:

1. Three different Avro products (Laptop, Smartphone, Headphones)
2. An integer key (42)
3. A complete sample event structure that can be used directly
