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
 */
package software.amazon.lambda.powertools.kafka.serializers;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.avro.util.ClassSecurityValidator;

/**
 * Deserializer for Kafka records using Avro format.
 */
public class KafkaAvroDeserializer extends AbstractKafkaDeserializer {

    private final Set<Class<?>> trustedTypes = ConcurrentHashMap.newKeySet();
    private final Set<String> trustedClassNames = ConcurrentHashMap.newKeySet();

    @Override
    protected <T> T deserializeObject(byte[] data, Class<T> type, SchemaRegistryType schemaRegistryType)
            throws IOException {
        // If no Avro generated class is passed we cannot deserialize using Avro
        if (SpecificRecordBase.class.isAssignableFrom(type)) {
            try {
                trust(type);
                DatumReader<T> datumReader = new SpecificDatumReader<>(type);
                Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);

                return datumReader.read(null, decoder);
            } catch (Exception e) {
                throw new IOException("Failed to deserialize Avro data.", e);
            }
        } else {
            throw new IOException("Unsupported type for Avro deserialization: " + type.getName() + ". "
                    + "Avro deserialization requires a type of org.apache.avro.specific.SpecificRecord. "
                    + "Consider using an alternative Deserializer.");
        }
    }

    // Avro 1.12.2+ only loads classes trusted by ClassSecurityValidator. We decode with the schema compiled into the
    // declared type and never with a schema from the record, so Avro only loads classes the developer authored.
    private void trust(Class<?> type) {
        if (trustedTypes.contains(type)) {
            return;
        }
        synchronized (this) {
            if (trustedTypes.isEmpty()) {
                ClassSecurityValidator.setGlobal(ClassSecurityValidator.composite(ClassSecurityValidator.getGlobal(),
                        clazz -> trustedClassNames.contains(clazz.getName())));
            }
            collectNamedTypes(SpecificData.get().getSchema(type));
            trustedTypes.add(type);
        }
    }

    private void collectNamedTypes(Schema schema) {
        switch (schema.getType()) {
            case RECORD -> {
                // Skip records we have already seen to stop on recursive schemas
                if (trustedClassNames.add(SpecificData.getClassName(schema))) {
                    schema.getFields().forEach(field -> collectNamedTypes(field.schema()));
                }
            }
            case ENUM, FIXED -> trustedClassNames.add(SpecificData.getClassName(schema));
            case ARRAY -> collectNamedTypes(schema.getElementType());
            case MAP -> collectNamedTypes(schema.getValueType());
            case UNION -> schema.getTypes().forEach(this::collectNamedTypes);
            default -> {
            }
        }
    }
}
