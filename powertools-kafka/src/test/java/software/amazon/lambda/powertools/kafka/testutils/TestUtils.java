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
package software.amazon.lambda.powertools.kafka.testutils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

/**
 * Utility class for common test functions
 */
public class TestUtils {
    
    /**
     * Helper method to create a ParameterizedType for ConsumerRecords
     * 
     * @param keyClass The class for the key type
     * @param valueClass The class for the value type
     * @return A Type representing ConsumerRecords<K, V>
     */
    public static Type createConsumerRecordsType(final Class<?> keyClass, final Class<?> valueClass) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { keyClass, valueClass };
            }

            @Override
            public Type getRawType() {
                return ConsumerRecords.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }
    
    /**
     * Helper method to serialize an Avro object
     * 
     * @param <T> The type of the Avro record
     * @param consumerRecord The Avro record to serialize
     * @return The serialized bytes
     * @throws IOException If serialization fails
     */
    public static <T extends SpecificRecord> byte[] serializeAvro(T consumerRecord) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(baos, null);
        @SuppressWarnings("unchecked")
        DatumWriter<T> writer = new SpecificDatumWriter<>((Class<T>) consumerRecord.getClass());
        writer.write(consumerRecord, encoder);
        encoder.flush();
        return baos.toByteArray();
    }
}
