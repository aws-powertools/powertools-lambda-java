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

package software.amazon.lambda.powertools.utilities;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.lambda.powertools.utilities.EventDeserializer.extractDataFrom;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.ActiveMQEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsFirehoseInputPreprocessingEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsStreamsInputPreprocessingEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import com.amazonaws.services.lambda.runtime.events.RabbitMQEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Invoke-primes {@link EventDeserializer} by running {@code extractDataFrom} plus Jackson
 * {@code as}/{@code asListOf} for every built-in event type.
 */
final class EventDeserializerPriming {

    static final String SAMPLE_JSON = "{\"id\":1234,\"name\":\"product\",\"price\":42}";

    private EventDeserializerPriming() {
    }

    static Set<Class<?>> prime() {
        Objects.requireNonNull(JsonConfig.get().getObjectMapper());
        Objects.requireNonNull(JsonConfig.get().getJmesPath());

        String sampleBase64 = Base64.getEncoder().encodeToString(SAMPLE_JSON.getBytes(UTF_8));
        Map<String, Object> sampleMap = Map.of("id", 1234, "name", "product", "price", 42);

        Set<Class<?>> primed = new LinkedHashSet<>();
        primeAs(primed, String.class, SAMPLE_JSON);
        primeAs(primed, Map.class, sampleMap);
        // Warm the JSON-array asListOf path (same source type as a raw String event)
        Objects.requireNonNull(extractDataFrom("[" + SAMPLE_JSON + "]").asListOf(Map.class));

        APIGatewayProxyRequestEvent apiV1 = new APIGatewayProxyRequestEvent();
        apiV1.setBody(SAMPLE_JSON);
        primeAs(primed, APIGatewayProxyRequestEvent.class, apiV1);

        APIGatewayV2HTTPEvent apiV2 = new APIGatewayV2HTTPEvent();
        apiV2.setBody(SAMPLE_JSON);
        primeAs(primed, APIGatewayV2HTTPEvent.class, apiV2);

        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage(SAMPLE_JSON);
        SNSEvent.SNSRecord snsRecord = new SNSEvent.SNSRecord();
        snsRecord.setSns(sns);
        SNSEvent snsEvent = new SNSEvent();
        snsEvent.setRecords(List.of(snsRecord));
        primeAs(primed, SNSEvent.class, snsEvent);

        SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
        sqsMessage.setBody(SAMPLE_JSON);
        SQSEvent sqsEvent = new SQSEvent();
        sqsEvent.setRecords(List.of(sqsMessage));
        primeAsList(primed, SQSEvent.class, sqsEvent);
        primeAs(primed, SQSEvent.SQSMessage.class, sqsMessage);

        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setDetail(sampleMap);
        primeAs(primed, ScheduledEvent.class, scheduledEvent);

        ApplicationLoadBalancerRequestEvent albEvent = new ApplicationLoadBalancerRequestEvent();
        albEvent.setBody(SAMPLE_JSON);
        primeAs(primed, ApplicationLoadBalancerRequestEvent.class, albEvent);

        CloudWatchLogsEvent.AWSLogs awsLogs = new CloudWatchLogsEvent.AWSLogs();
        awsLogs.setData(sampleBase64);
        CloudWatchLogsEvent cloudWatchLogsEvent = new CloudWatchLogsEvent();
        cloudWatchLogsEvent.setAwsLogs(awsLogs);
        primeAs(primed, CloudWatchLogsEvent.class, cloudWatchLogsEvent);

        CloudFormationCustomResourceEvent cloudFormationEvent = new CloudFormationCustomResourceEvent();
        cloudFormationEvent.setResourceProperties(sampleMap);
        primeAs(primed, CloudFormationCustomResourceEvent.class, cloudFormationEvent);

        KinesisEvent.KinesisEventRecord kinesisEventRecord = kinesisEventRecord();
        KinesisEvent kinesisEvent = new KinesisEvent();
        kinesisEvent.setRecords(List.of(kinesisEventRecord));
        primeAsList(primed, KinesisEvent.class, kinesisEvent);
        // Use a fresh record: decode(ByteBuffer) consumes the buffer position
        primeAs(primed, KinesisEvent.KinesisEventRecord.class, kinesisEventRecord());

        KinesisFirehoseEvent.Record firehoseRecord = new KinesisFirehoseEvent.Record();
        firehoseRecord.setData(jsonBuffer());
        KinesisFirehoseEvent firehoseEvent = new KinesisFirehoseEvent();
        firehoseEvent.setRecords(List.of(firehoseRecord));
        primeAsList(primed, KinesisFirehoseEvent.class, firehoseEvent);

        KafkaEvent.KafkaEventRecord kafkaRecord = new KafkaEvent.KafkaEventRecord();
        kafkaRecord.setValue(sampleBase64);
        KafkaEvent kafkaEvent = new KafkaEvent();
        kafkaEvent.setRecords(Map.of("topic", List.of(kafkaRecord)));
        primeAsList(primed, KafkaEvent.class, kafkaEvent);

        ActiveMQEvent.ActiveMQMessage activeMqMessage = new ActiveMQEvent.ActiveMQMessage();
        activeMqMessage.setData(sampleBase64);
        ActiveMQEvent activeMqEvent = new ActiveMQEvent();
        activeMqEvent.setMessages(List.of(activeMqMessage));
        primeAsList(primed, ActiveMQEvent.class, activeMqEvent);

        RabbitMQEvent.RabbitMessage rabbitMessage = new RabbitMQEvent.RabbitMessage();
        rabbitMessage.setData(sampleBase64);
        RabbitMQEvent rabbitMqEvent = new RabbitMQEvent();
        rabbitMqEvent.setRmqMessagesByQueue(Map.of("queue", List.of(rabbitMessage)));
        primeAsList(primed, RabbitMQEvent.class, rabbitMqEvent);

        KinesisAnalyticsFirehoseInputPreprocessingEvent.Record kaFirehoseRecord =
                new KinesisAnalyticsFirehoseInputPreprocessingEvent.Record();
        kaFirehoseRecord.setData(jsonBuffer());
        KinesisAnalyticsFirehoseInputPreprocessingEvent kaFirehoseEvent =
                new KinesisAnalyticsFirehoseInputPreprocessingEvent();
        kaFirehoseEvent.setRecords(List.of(kaFirehoseRecord));
        primeAsList(primed, KinesisAnalyticsFirehoseInputPreprocessingEvent.class, kaFirehoseEvent);

        KinesisAnalyticsStreamsInputPreprocessingEvent.Record kaStreamsRecord =
                new KinesisAnalyticsStreamsInputPreprocessingEvent.Record();
        kaStreamsRecord.setData(jsonBuffer());
        KinesisAnalyticsStreamsInputPreprocessingEvent kaStreamsEvent =
                new KinesisAnalyticsStreamsInputPreprocessingEvent();
        kaStreamsEvent.setRecords(List.of(kaStreamsRecord));
        primeAsList(primed, KinesisAnalyticsStreamsInputPreprocessingEvent.class, kaStreamsEvent);

        return primed;
    }

    private static void primeAs(Set<Class<?>> primed, Class<?> eventType, Object event) {
        Objects.requireNonNull(extractDataFrom(event).as(Map.class));
        primed.add(eventType);
    }

    private static void primeAsList(Set<Class<?>> primed, Class<?> eventType, Object event) {
        Objects.requireNonNull(extractDataFrom(event).asListOf(Map.class));
        primed.add(eventType);
    }

    private static ByteBuffer jsonBuffer() {
        return ByteBuffer.wrap(SAMPLE_JSON.getBytes(UTF_8));
    }

    private static KinesisEvent.KinesisEventRecord kinesisEventRecord() {
        KinesisEvent.Record kinesisRecord = new KinesisEvent.Record();
        kinesisRecord.setData(jsonBuffer());
        KinesisEvent.KinesisEventRecord kinesisEventRecord = new KinesisEvent.KinesisEventRecord();
        kinesisEventRecord.setKinesis(kinesisRecord);
        return kinesisEventRecord;
    }
}
