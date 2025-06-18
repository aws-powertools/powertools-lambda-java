package org.demo.kafka.tools;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class to generate base64-encoded JSON serialized products
 * for use in test events.
 */
public class GenerateJsonSamples {

    public static void main(String[] args) throws IOException {
        // Create three different products
        Map<String, Object> product1 = new HashMap<>();
        product1.put("id", 1001);
        product1.put("name", "Laptop");
        product1.put("price", 999.99);

        Map<String, Object> product2 = new HashMap<>();
        product2.put("id", 1002);
        product2.put("name", "Smartphone");
        product2.put("price", 599.99);

        Map<String, Object> product3 = new HashMap<>();
        product3.put("id", 1003);
        product3.put("name", "Headphones");
        product3.put("price", 149.99);

        // Serialize and encode each product
        String encodedProduct1 = serializeAndEncode(product1);
        String encodedProduct2 = serializeAndEncode(product2);
        String encodedProduct3 = serializeAndEncode(product3);

        // Serialize and encode an integer key
        String encodedKey = serializeAndEncodeInteger(42);

        // Print the results
        System.out.println("Base64 encoded JSON products for use in kafka-json-event.json:");
        System.out.println("\nProduct 1 (with key):");
        System.out.println("key: \"" + encodedKey + "\",");
        System.out.println("value: \"" + encodedProduct1 + "\",");

        System.out.println("\nProduct 2 (with key):");
        System.out.println("key: \"" + encodedKey + "\",");
        System.out.println("value: \"" + encodedProduct2 + "\",");

        System.out.println("\nProduct 3 (without key):");
        System.out.println("key: null,");
        System.out.println("value: \"" + encodedProduct3 + "\",");

        // Print a sample event structure
        System.out.println("\nSample event structure:");
        printSampleEvent(encodedKey, encodedProduct1, encodedProduct2, encodedProduct3);
    }

    private static String serializeAndEncode(Map<String, Object> product) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(product);
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    private static String serializeAndEncodeInteger(Integer value) {
        // For simple types like integers, we'll just convert to string and encode
        return Base64.getEncoder().encodeToString(value.toString().getBytes());
    }

    private static void printSampleEvent(String key, String product1, String product2, String product3) {
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
                "        \"value\": \"" + product1 + "\",\n" +
                "        \"headers\": [\n" +
                "          {\n" +
                "            \"headerKey\": [104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101]\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"topic\": \"mytopic\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 15,\n" +
                "        \"timestamp\": 1545084650987,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": \"" + key + "\",\n" +
                "        \"value\": \"" + product2 + "\",\n" +
                "        \"headers\": [\n" +
                "          {\n" +
                "            \"headerKey\": [104, 101, 97, 100, 101, 114, 86, 97, 108, 117, 101]\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"topic\": \"mytopic\",\n" +
                "        \"partition\": 0,\n" +
                "        \"offset\": 15,\n" +
                "        \"timestamp\": 1545084650987,\n" +
                "        \"timestampType\": \"CREATE_TIME\",\n" +
                "        \"key\": null,\n" +
                "        \"value\": \"" + product3 + "\",\n" +
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
