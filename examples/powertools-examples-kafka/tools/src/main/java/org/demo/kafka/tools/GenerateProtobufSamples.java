package org.demo.kafka.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

import org.apache.kafka.common.utils.ByteUtils;
import org.demo.kafka.protobuf.ProtobufProduct;

import com.google.protobuf.CodedOutputStream;

/**
 * Utility class to generate base64-encoded Protobuf serialized products
 * for use in test events.
 */
public final class GenerateProtobufSamples {

    private GenerateProtobufSamples() {
        // Utility class
    }

    public static void main(String[] args) throws IOException {
        // Create a single product that will be used for all four scenarios
        ProtobufProduct product = ProtobufProduct.newBuilder()
                .setId(1001)
                .setName("Laptop")
                .setPrice(999.99)
                .build();

        // Create four different serializations of the same product
        String standardProduct = serializeAndEncode(product);
        String productWithConfluentSimpleIndex = serializeWithConfluentSimpleMessageIndex(product);
        String productWithConfluentComplexIndex = serializeWithConfluentComplexMessageIndex(product);
        String productWithGlueMagicByte = serializeWithGlueMagicByte(product);

        // Serialize and encode an integer key (same for all records)
        String encodedKey = serializeAndEncodeInteger(42);

        // Print the results
        System.out.println("Base64 encoded Protobuf products with different scenarios:");
        System.out.println("\n1. Plain Protobuf (no schema registry):");
        System.out.println("value: \"" + standardProduct + "\"");

        System.out.println("\n2. Confluent with Simple Message Index (optimized single 0):");
        System.out.println("value: \"" + productWithConfluentSimpleIndex + "\"");

        System.out.println("\n3. Confluent with Complex Message Index (array [1,0]):");
        System.out.println("value: \"" + productWithConfluentComplexIndex + "\"");

        System.out.println("\n4. Glue with Magic Byte:");
        System.out.println("value: \"" + productWithGlueMagicByte + "\"");

        // Print the merged event structure
        System.out.println("\n" + "=".repeat(80));
        System.out.println("MERGED EVENT WITH ALL FOUR SCENARIOS");
        System.out.println("=".repeat(80));
        printSampleEvent(encodedKey, standardProduct, productWithConfluentSimpleIndex, productWithConfluentComplexIndex,
                productWithGlueMagicByte);
    }

    private static String serializeAndEncode(ProtobufProduct product) {
        return Base64.getEncoder().encodeToString(product.toByteArray());
    }

    /**
     * Serializes a protobuf product with a simple Confluent message index (optimized single 0).
     * Format: [0][protobuf_data]
     * 
     * @see {@link https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format}
     */
    private static String serializeWithConfluentSimpleMessageIndex(ProtobufProduct product) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write optimized simple message index for Confluent (single 0 byte for [0])
        baos.write(0);

        // Write the protobuf data
        baos.write(product.toByteArray());

        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Serializes a protobuf product with a complex Confluent message index (array [1,0]).
     * Format: [2][1][0][protobuf_data] where 2 is the array length using varint encoding
     * 
     * @see {@link https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format}
     */
    private static String serializeWithConfluentComplexMessageIndex(ProtobufProduct product) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write complex message index array [1,0] using ByteUtils
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        ByteUtils.writeVarint(2, buffer); // Array length
        ByteUtils.writeVarint(1, buffer); // First index value
        ByteUtils.writeVarint(0, buffer); // Second index value

        buffer.flip();
        byte[] indexData = new byte[buffer.remaining()];
        buffer.get(indexData);
        baos.write(indexData);

        // Write the protobuf data
        baos.write(product.toByteArray());

        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Serializes a protobuf product with Glue magic byte.
     * Format: [1][protobuf_data] where 1 is the magic byte
     */
    private static String serializeWithGlueMagicByte(ProtobufProduct product) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream codedOutput = CodedOutputStream.newInstance(baos);

        // Write Glue magic byte (single UInt32)
        codedOutput.writeUInt32NoTag(1);

        // Write the protobuf data
        product.writeTo(codedOutput);

        codedOutput.flush();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private static String serializeAndEncodeInteger(Integer value) {
        // For simple types like integers, we'll just convert to string and encode
        return Base64.getEncoder().encodeToString(value.toString().getBytes());
    }

    private static void printSampleEvent(String key, String standardProduct, String confluentSimpleProduct,
            String confluentComplexProduct, String glueProduct) {
        System.out.println("{\n" +
                "  \"eventSource\": \"aws:kafka\",\n" +
                "  \"eventSourceArn\": \"arn:aws:kafka:us-east-1:0123456789019:cluster/SalesCluster/abcd1234-abcd-cafe-abab-9876543210ab-4\",\n"
                +
                "  \"bootstrapServers\": \"b-2.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092,b-1.demo-cluster-1.a1bcde.c1.kafka.us-east-1.amazonaws.com:9092\",\n"
                +
                "  \"records\": {\n" +
                "    \"mytopic-0\": [\n" +
                "      {\n" +
                "        \"topic\": \"mytopic\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 15,\n" +
                "        \"timestamp\": 1545084650987,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": \"" + key + "\",\n" +
                "        \"value\": \"" + standardProduct + "\",\n" +
                "        \"headers\": [\n" +
                "          {\n" +
                "            \"headerKey\": [104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101]\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"topic\": \"mytopic\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 16,\n" +
                "        \"timestamp\": 1545084650988,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": \"" + key + "\",\n" +
                "        \"value\": \"" + confluentSimpleProduct + "\",\n" +
                "        \"headers\": [\n" +
                "          {\n" +
                "            \"headerKey\": [104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101]\n" +
                "          }\n" +
                "        ],\n" +
                "        \"valueSchemaMetadata\": {\n" +
                "          \"schemaId\": \"123\",\n" +
                "          \"dataFormat\": \"PROTOBUF\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"topic\": \"mytopic\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 17,\n" +
                "        \"timestamp\": 1545084650989,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": null,\n" +
                "        \"value\": \"" + confluentComplexProduct + "\",\n" +
                "        \"headers\": [\n" +
                "          {\n" +
                "            \"headerKey\": [104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101]\n" +
                "          }\n" +
                "        ],\n" +
                "        \"valueSchemaMetadata\": {\n" +
                "          \"schemaId\": \"456\",\n" +
                "          \"dataFormat\": \"PROTOBUF\"\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"topic\": \"mytopic\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 18,\n" +
                "        \"timestamp\": 1545084650990,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": \"" + key + "\",\n" +
                "        \"value\": \"" + glueProduct + "\",\n" +
                "        \"headers\": [\n" +
                "          {\n" +
                "            \"headerKey\": [104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101]\n" +
                "          }\n" +
                "        ],\n" +
                "        \"valueSchemaMetadata\": {\n" +
                "          \"schemaId\": \"12345678-1234-1234-1234-123456789012\",\n" +
                "          \"dataFormat\": \"PROTOBUF\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");
    }
}
