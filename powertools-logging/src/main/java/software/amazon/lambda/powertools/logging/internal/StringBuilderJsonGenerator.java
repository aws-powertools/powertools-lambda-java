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

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.lambda.powertools.utilities.JsonConfig;

/**
 * Partial implementation of {@link JsonGenerator} that writes to a {@link StringBuilder}.
 * Used internally for json formatting, not to be used externally
 */
public class StringBuilderJsonGenerator extends JsonGenerator {

    private final StringBuilder builder;

    public StringBuilderJsonGenerator(StringBuilder builder) {
        super();
        if (builder == null) {
            throw new IllegalArgumentException("StringBuilder cannot be null");
        }
        this.builder = builder;
    }

    @Override
    public JsonGenerator setCodec(ObjectCodec oc) {
        return this;
    }

    @Override
    public ObjectCodec getCodec() {
        return null;
    }

    @Override
    public Version version() {
        return new Version(2, 0, 0, "", "software.amazon.lambda", "powertools");
    }

    @Override
    public JsonStreamContext getOutputContext() {
        return null;
    }

    @Override
    public JsonGenerator enable(Feature f) {
        return this;
    }

    @Override
    public JsonGenerator disable(Feature f) {
        return this;
    }

    @Override
    public boolean isEnabled(Feature f) {
        return false;
    }

    @Override
    public int getFeatureMask() {
        return 0;
    }

    @Override
    public JsonGenerator setFeatureMask(int values) {
        return this;
    }

    @Override
    public JsonGenerator useDefaultPrettyPrinter() {
        return this;
    }

    @Override
    public void writeStartArray() {
        builder.append('[');
    }

    @Override
    public void writeEndArray() {
        builder.append(']');
    }

    @Override
    public void writeStartObject() {
        builder.append('{');
    }

    @Override
    public void writeEndObject() {
        builder.append('}');
    }

    @Override
    public void writeFieldName(String name) {
        builder.append("\"").append(name).append("\":");
    }

    @Override
    public void writeFieldName(SerializableString name) {
        builder.append("\"").append(name.getValue()).append("\":");
    }

    @Override
    public void writeString(String text) {
        builder.append("\"").append(text).append("\"");
    }

    @Override
    public void writeString(char[] buffer, int offset, int len) {
        builder.append("\"").append(buffer, offset, len).append("\"");
    }

    @Override
    public void writeString(SerializableString text) {
        builder.append("\"").append(text.getValue()).append("\"");
    }

    @Override
    public void writeRawUTF8String(byte[] buffer, int offset, int len) {
        builder.append(new String(buffer, offset, len, StandardCharsets.UTF_8));
    }

    @Override
    public void writeUTF8String(byte[] buffer, int offset, int len) {
        builder.append("\"").append(new String(buffer, offset, len, StandardCharsets.UTF_8)).append("\"");
    }

    @Override
    public void writeRaw(String text) {
        builder.append(text);
    }

    @Override
    public void writeRaw(String text, int offset, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeRaw(char c) {
        builder.append(c);
    }

    @Override
    public void writeRawValue(String text) {
        builder.append(text);
    }

    @Override
    public void writeRawValue(String text, int offset, int len) {
        builder.append(text, offset, len);
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) {
        builder.append(text, offset, len);
    }

    @Override
    public void writeBinary(byte[] data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBinary(Base64Variant bv, byte[] data, int offset, int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int writeBinary(Base64Variant bv, InputStream data, int dataLength) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeNumber(short v) {
        builder.append(v);
    }

    @Override
    public void writeNumber(int v) {
        builder.append(v);
    }

    @Override
    public void writeNumber(long v) {
        builder.append(v);
    }

    @Override
    public void writeNumber(BigInteger v) {
        builder.append(v);
    }

    @Override
    public void writeNumber(double v) {
        builder.append(v);
    }

    @Override
    public void writeNumber(float v) {
        builder.append(v);
    }

    @Override
    public void writeNumber(BigDecimal v) {
        builder.append(v.toPlainString());
    }

    @Override
    public void writeNumber(String encodedValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBoolean(boolean state) {
        builder.append(state);
    }

    @Override
    public void writeNull() {
        builder.append("null");
    }

    @Override
    public void writeObject(Object value) throws IOException {
        if (value == null) {
            writeNull();
            return;
        }
        if (value instanceof String) {
            writeString((String) value);
            return;
        }
        if (value instanceof Number) {
            Number n = (Number) value;
            if (n instanceof Integer) {
                writeNumber(n.intValue());
                return;
            }
            if (n instanceof Long) {
                writeNumber(n.longValue());
                return;
            }
            if (n instanceof Double) {
                writeNumber(n.doubleValue());
                return;
            }
            if (n instanceof Float) {
                writeNumber(n.floatValue());
                return;
            }
            if (n instanceof Short) {
                writeNumber(n.shortValue());
                return;
            }
            if (n instanceof Byte) {
                writeNumber(n.byteValue());
                return;
            }
            if (n instanceof BigInteger) {
                writeNumber((BigInteger) n);
                return;
            }
            if (n instanceof BigDecimal) {
                writeNumber((BigDecimal) n);
                return;
            }
            if (n instanceof AtomicInteger) {
                writeNumber(((AtomicInteger) n).get());
                return;
            }
            if (n instanceof AtomicLong) {
                writeNumber(((AtomicLong) n).get());
                return;
            }
        }
        if (value instanceof byte[]) {
            writeBinary((byte[]) value);
            return;
        }
        if (value instanceof Boolean) {
            writeBoolean((Boolean) value);
            return;
        }
        if (value instanceof AtomicBoolean) {
            writeBoolean(((AtomicBoolean) value).get());
            return;
        }
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;

            switch (node.getNodeType()) {
                case NULL:
                case MISSING:
                    writeNull();
                    return;

                case STRING:
                    writeString(node.asText());
                    return;

                case BOOLEAN:
                    writeBoolean(node.asBoolean());
                    return;

                case BINARY:
                    writeBinary(node.binaryValue());
                    return;

                case NUMBER:
                    if (node.isInt()) {
                        writeNumber(node.intValue());
                        return;
                    }
                    if (node.isLong()) {
                        writeNumber(node.longValue());
                        return;
                    }
                    if (node.isShort()) {
                        writeNumber(node.shortValue());
                        return;
                    }
                    if (node.isDouble()) {
                        writeNumber(node.doubleValue());
                        return;
                    }
                    if (node.isFloat()) {
                        writeNumber(node.floatValue());
                        return;
                    }
                    if (node.isBigDecimal()) {
                        writeNumber(node.decimalValue());
                        return;
                    }
                    if (node.isBigInteger()) {
                        writeNumber(node.bigIntegerValue());
                    }
                    return;
                case OBJECT:
                case POJO:
                    writeStartObject();
                    for (Iterator<Map.Entry<String, JsonNode>> entries = node.fields(); entries.hasNext(); ) {
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
                    int size = arrayNode.size();
                    writeStartArray(arrayNode, size);
                    for (Iterator<JsonNode> elements = arrayNode.elements(); elements.hasNext(); ) {
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
        }
        try {
            // default: try to write object as JSON
            writeRawValue(JsonConfig.get().getObjectMapper().writeValueAsString(value));
        } catch (Exception e) {
            // last chance: toString
            writeString(value.toString());
        }
    }

    @Override
    public void writeTree(TreeNode rootNode) throws IOException {
        if (rootNode == null) {
            writeNull();
        } else if (rootNode instanceof TextNode) {
            writeString(((TextNode) rootNode).asText());
        } else if  (rootNode instanceof BooleanNode) {
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
        } else if (rootNode instanceof BinaryNode) {
            writeBinary(((BinaryNode) rootNode).binaryValue());
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
    public void flush() {
        // nothing to do
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() {
        // nothing to do
    }
}


