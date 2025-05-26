/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.logging.internal;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.lambda.powertools.utilities.JsonConfig;

/**
 * A simple JSON serializer.
 * Used internally for json serialization, not to be used externally.
 * We do not use Jackson as we need to serialize each fields of the log event individually.
 * Mainly used by logback as log4j is using its own JsonWriter
 */
public class JsonSerializer implements AutoCloseable {

    private final StringBuilder builder;

    public JsonSerializer(StringBuilder builder) {
        super();
        if (builder == null) {
            throw new IllegalArgumentException("StringBuilder cannot be null");
        }
        this.builder = builder;
    }

    public void writeStartArray() {
        builder.append('[');
    }

    public void writeEndArray() {
        builder.append(']');
    }

    public void writeStartObject() {
        builder.append('{');
    }

    public void writeEndObject() {
        builder.append('}');
    }

    public void writeSeparator() {
        writeRaw(',');
    }

    public void writeFieldName(String name) {
        Objects.requireNonNull(name, "field name must not be null");
        writeString(name);
        writeRaw(':');
    }

    public void writeString(String text) {
        if (text == null) {
            writeNull();
        } else {
            // Escape double quotes to avoid breaking JSON format
            builder.append("\"").append(text.replace("\"", "\\\"")).append("\"");
        }
    }

    public void writeRaw(String text) {
        builder.append(text);
    }

    public void writeRaw(char c) {
        builder.append(c);
    }

    public void writeNumber(short v) {
        builder.append(v);
    }

    public void writeNumber(int v) {
        builder.append(v);
    }

    public void writeNumber(long v) {
        builder.append(v);
    }

    public void writeNumber(BigInteger v) {
        builder.append(v);
    }

    public void writeNumber(double v) {
        builder.append(v);
    }

    public void writeNumber(float v) {
        builder.append(v);
    }

    public void writeNumber(BigDecimal v) {
        builder.append(v.toPlainString());
    }

    public void writeBoolean(boolean state) {
        builder.append(state);
    }

    public void writeArray(final char[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeStartArray();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                builder.append('\'');
                builder.append(items[itemIndex]);
                builder.append('\'');
            }
            writeEndArray();
        }
    }

    public void writeArray(final boolean[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeStartArray();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final boolean item = items[itemIndex];
                writeBoolean(item);
            }
            writeEndArray();
        }
    }

    public void writeArray(final byte[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeStartArray();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final byte item = items[itemIndex];
                writeNumber(item);
            }
            writeEndArray();
        }
    }

    public void writeArray(final short[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeStartArray();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final short item = items[itemIndex];
                writeNumber(item);
            }
            writeEndArray();
        }
    }

    public void writeArray(final int[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeStartArray();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final int item = items[itemIndex];
                writeNumber(item);
            }
            writeEndArray();
        }
    }

    public void writeArray(final long[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeStartArray();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final long item = items[itemIndex];
                writeNumber(item);
            }
            writeEndArray();
        }
    }

    public void writeArray(final float[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeStartArray();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final float item = items[itemIndex];
                writeNumber(item);
            }
            writeEndArray();
        }
    }

    public void writeArray(final double[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeStartArray();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final double item = items[itemIndex];
                writeNumber(item);
            }
            writeEndArray();
        }
    }

    public void writeArray(final Object[] items) {
        if (items == null) {
            writeNull();
        } else {
            writeStartArray();
            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final Object item = items[itemIndex];
                writeObject(item);
            }
            writeEndArray();
        }
    }

    public void writeNull() {
        builder.append("null");
    }

    public void writeArray(final List<?> items) {
        if (items == null) {
            writeNull();
        } else {
            writeStartArray();
            for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                if (itemIndex > 0) {
                    writeSeparator();
                }
                final Object item = items.get(itemIndex);
                writeObject(item);
            }
            writeEndArray();
        }
    }

    public void writeArray(final Collection<?> items) {
        if (items == null) {
            writeNull();
        } else {
            writeStartArray();
            Iterator<?> iterator = items.iterator();
            while (iterator.hasNext()) {
                writeObject(iterator.next());
                if (iterator.hasNext()) {
                    writeSeparator();
                }
            }
            writeEndArray();
        }
    }

    public void writeMap(final Map<?, ?> map) {
        if (map == null) {
            writeNull();
        } else {
            writeStartObject();
            for (Iterator<? extends Map.Entry<?, ?>> entries = map.entrySet().iterator(); entries.hasNext();) {
                Map.Entry<?, ?> entry = entries.next();
                writeObjectField(String.valueOf(entry.getKey()), entry.getValue());
                if (entries.hasNext()) {
                    builder.append(',');
                }
            }
            writeEndObject();
        }
    }

    public void writeObject(Object value) {

        // null
        if (value == null) {
            writeNull();
        }

        else if (value instanceof String) {
            writeString((String) value);
        }

        // number & boolean
        else if (value instanceof Number) {
            Number n = (Number) value;
            if (n instanceof Integer) {
                writeNumber(n.intValue());
            } else if (n instanceof Long) {
                writeNumber(n.longValue());
            } else if (n instanceof Double) {
                writeNumber(n.doubleValue());
            } else if (n instanceof Float) {
                writeNumber(n.floatValue());
            } else if (n instanceof Short) {
                writeNumber(n.shortValue());
            } else if (n instanceof Byte) {
                writeNumber(n.byteValue());
            } else if (n instanceof BigInteger) {
                writeNumber((BigInteger) n);
            } else if (n instanceof BigDecimal) {
                writeNumber((BigDecimal) n);
            } else if (n instanceof AtomicInteger) {
                writeNumber(((AtomicInteger) n).get());
            } else if (n instanceof AtomicLong) {
                writeNumber(((AtomicLong) n).get());
            }
        } else if (value instanceof Boolean) {
            writeBoolean((Boolean) value);
        } else if (value instanceof AtomicBoolean) {
            writeBoolean(((AtomicBoolean) value).get());
        }

        // list & collection
        else if (value instanceof List) {
            final List<?> list = (List<?>) value;
            writeArray(list);
        } else if (value instanceof Collection) {
            final Collection<?> collection = (Collection<?>) value;
            writeArray(collection);
        }

        // map
        else if (value instanceof Map) {
            final Map<?, ?> map = (Map<?, ?>) value;
            writeMap(map);
        }

        // arrays
        else if (value instanceof char[]) {
            final char[] charValues = (char[]) value;
            writeArray(charValues);
        } else if (value instanceof boolean[]) {
            final boolean[] booleanValues = (boolean[]) value;
            writeArray(booleanValues);
        } else if (value instanceof byte[]) {
            final byte[] byteValues = (byte[]) value;
            writeArray(byteValues);
        } else if (value instanceof short[]) {
            final short[] shortValues = (short[]) value;
            writeArray(shortValues);
        } else if (value instanceof int[]) {
            final int[] intValues = (int[]) value;
            writeArray(intValues);
        } else if (value instanceof long[]) {
            final long[] longValues = (long[]) value;
            writeArray(longValues);
        } else if (value instanceof float[]) {
            final float[] floatValues = (float[]) value;
            writeArray(floatValues);
        } else if (value instanceof double[]) {
            final double[] doubleValues = (double[]) value;
            writeArray(doubleValues);
        } else if (value instanceof Object[]) {
            final Object[] values = (Object[]) value;
            writeArray(values);
        }

        else if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;

            switch (node.getNodeType()) {
                case NULL:
                case MISSING:
                    writeNull();
                    break;

                case STRING:
                    writeString(node.asText());
                    break;

                case BOOLEAN:
                    writeBoolean(node.asBoolean());
                    break;

                case NUMBER:
                    if (node.isInt()) {
                        writeNumber(node.intValue());
                        break;
                    }
                    if (node.isLong()) {
                        writeNumber(node.longValue());
                        break;
                    }
                    if (node.isShort()) {
                        writeNumber(node.shortValue());
                        break;
                    }
                    if (node.isDouble()) {
                        writeNumber(node.doubleValue());
                        break;
                    }
                    if (node.isFloat()) {
                        writeNumber(node.floatValue());
                        break;
                    }
                    if (node.isBigDecimal()) {
                        writeNumber(node.decimalValue());
                        break;
                    }
                    if (node.isBigInteger()) {
                        writeNumber(node.bigIntegerValue());
                        break;
                    }
                    break;
                case OBJECT:
                case POJO:
                    writeStartObject();
                    for (Iterator<Map.Entry<String, JsonNode>> entries = node.fields(); entries.hasNext();) {
                        Map.Entry<String, JsonNode> entry = entries.next();
                        writeObjectField(entry.getKey(), entry.getValue());
                        if (entries.hasNext()) {
                            builder.append(',');
                        }
                    }
                    writeEndObject();
                    return;

                case ARRAY:
                    ArrayNode arrayNode = (ArrayNode) node;
                    writeStartArray();
                    for (Iterator<JsonNode> elements = arrayNode.elements(); elements.hasNext();) {
                        writeObject(elements.next());
                        if (elements.hasNext()) {
                            builder.append(',');
                        }
                    }
                    writeEndArray();
                    return;

                default:
                    break;
            }
        } else {
            try {
                // default: try to write object as JSON
                writeRaw(JsonConfig.get().getObjectMapper().writeValueAsString(value));
            } catch (Exception e) {
                // last chance: toString
                writeString(value.toString());
            }
        }
    }

    public void writeObjectField(String key, Object value) {
        writeFieldName(key);
        writeObject(value);
    }

    public void writeBooleanField(String key, boolean value) {
        writeFieldName(key);
        writeBoolean(value);
    }

    public void writeNullField(String key) {
        writeFieldName(key);
        writeNull();
    }

    public void writeNumberField(String key, int value) {
        writeFieldName(key);
        writeNumber(value);
    }

    public void writeNumberField(String key, float value) {
        writeFieldName(key);
        writeNumber(value);
    }

    public void writeNumberField(String key, short value) {
        writeFieldName(key);
        writeNumber(value);
    }

    public void writeNumberField(String key, long value) {
        writeFieldName(key);
        writeNumber(value);
    }

    public void writeNumberField(String key, BigInteger value) {
        writeFieldName(key);
        writeNumber(value);
    }

    public void writeNumberField(String key, double value) {
        writeFieldName(key);
        writeNumber(value);
    }

    public void writeNumberField(String key, BigDecimal value) {
        writeFieldName(key);
        writeNumber(value);
    }

    public void writeStringField(String key, String value) {
        writeFieldName(key);
        writeString(value);
    }

    public void writeTree(TreeNode rootNode) {
        if (rootNode == null) {
            writeNull();
        } else if (rootNode instanceof TextNode) {
            writeString(((TextNode) rootNode).asText());
        } else if (rootNode instanceof BooleanNode) {
            writeBoolean(((BooleanNode) rootNode).asBoolean());
        } else if (rootNode instanceof NumericNode) {
            NumericNode numericNode = (NumericNode) rootNode;
            if (numericNode.isInt()) {
                writeNumber(numericNode.intValue());
            } else if (numericNode.isLong()) {
                writeNumber(numericNode.longValue());
            } else if (numericNode.isShort()) {
                writeNumber(numericNode.shortValue());
            } else if (numericNode.isDouble()) {
                writeNumber(numericNode.doubleValue());
            } else if (numericNode.isFloat()) {
                writeNumber(numericNode.floatValue());
            } else if (numericNode.isBigDecimal()) {
                writeNumber(numericNode.decimalValue());
            } else if (numericNode.isBigInteger()) {
                writeNumber(numericNode.bigIntegerValue());
            }
        } else if (rootNode instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) rootNode;
            writeObject(arrayNode);
        } else if (rootNode instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) rootNode;
            writeObject(objectNode);
        } else if (rootNode instanceof NullNode || rootNode instanceof MissingNode) {
            writeNull();
        } else if (rootNode instanceof POJONode) {
            writeObject(((POJONode) rootNode).getPojo());
        }
    }

    @Override
    public void close() {
        // nothing to do
    }
}
