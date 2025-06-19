package org.demo.kafka.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

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
        // Create a single product that will be used for all three scenarios
        ProtobufProduct product = ProtobufProduct.newBuilder()
                .setId(1001)
                .setName("Laptop")
                .setPrice(999.99)
                .build();

        // Create three different serializations of the same product
        String standardProduct = serializeAndEncode(product);
        String productWithSimpleIndex = serializeWithSimpleMessageIndex(product);
        String productWithComplexIndex = serializeWithComplexMessageIndex(product);

        // Serialize and encode an integer key (same for all records)
        String encodedKey = serializeAndEncodeInteger(42);

        // Print the results
        System.out.println("Base64 encoded Protobuf products with different message index scenarios:");
        System.out.println("\n1. Standard Protobuf (no message index):");
        System.out.println("value: \"" + standardProduct + "\"");

        System.out.println("\n2. Simple Message Index (single 0):");
        System.out.println("value: \"" + productWithSimpleIndex + "\"");

        System.out.println("\n3. Complex Message Index (array [1,0]):");
        System.out.println("value: \"" + productWithComplexIndex + "\"");

        // Print the merged event structure
        System.out.println("\n" + "=".repeat(80));
        System.out.println("MERGED EVENT WITH ALL THREE SCENARIOS");
        System.out.println("=".repeat(80));
        printSampleEvent(encodedKey, standardProduct, productWithSimpleIndex, productWithComplexIndex);
    }

    private static String serializeAndEncode(ProtobufProduct product) {
        return Base64.getEncoder().encodeToString(product.toByteArray());
    }

    /**
     * Serializes a protobuf product with a simple Confluent message index (single 0).
     * Format: [0][protobuf_data]
     * 
     * @see {@link https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format}
     */
    private static String serializeWithSimpleMessageIndex(ProtobufProduct product) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream codedOutput = CodedOutputStream.newInstance(baos);

        // Write simple message index (single 0)
        codedOutput.writeUInt32NoTag(0);

        // Write the protobuf data
        product.writeTo(codedOutput);

        codedOutput.flush();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Serializes a protobuf product with a complex Confluent message index (array [1,0]).
     * Format: [2][1][0][protobuf_data] where 2 is the array length
     * 
     * @see {@link https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/index.html#wire-format}
     */
    private static String serializeWithComplexMessageIndex(ProtobufProduct product) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream codedOutput = CodedOutputStream.newInstance(baos);

        // Write complex message index array [1,0]
        codedOutput.writeUInt32NoTag(2); // Array length
        codedOutput.writeUInt32NoTag(1); // First index value
        codedOutput.writeUInt32NoTag(0); // Second index value

        // Write the protobuf data
        product.writeTo(codedOutput);

        codedOutput.flush();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private static String serializeAndEncodeInteger(Integer value) {
        // For simple types like integers, we'll just convert to string and encode
        return Base64.getEncoder().encodeToString(value.toString().getBytes());
    }

    private static void printSampleEvent(String key, String standardProduct, String simpleIndexProduct,
            String complexIndexProduct) {
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
                "        \"value\": \"" + simpleIndexProduct + "\",\n" +
                "        \"headers\": [\n" +
                "          {\n" +
                "            \"headerKey\": [104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101]\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"topic\": \"mytopic\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 17,\n" +
                "        \"timestamp\": 1545084650989,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": null,\n" +
                "        \"value\": \"" + complexIndexProduct + "\",\n" +
                "        \"headers\": [\n" +
                "          {\n" +
                "            \"headerKey\": [104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");
    }
}
