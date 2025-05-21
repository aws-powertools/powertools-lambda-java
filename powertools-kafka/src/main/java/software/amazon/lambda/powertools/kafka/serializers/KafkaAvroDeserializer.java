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

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;

/**
 * Deserializer for Kafka records using Avro format.
 */
public class KafkaAvroDeserializer extends AbstractKafkaDeserializer {

    @Override
    protected <T> T deserializeComplex(byte[] data, Class<T> type) throws IOException {
        // If no Avro generated class is passed we cannot deserialize using Avro
        if (SpecificRecordBase.class.isAssignableFrom(type)) {
            try {
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
}
