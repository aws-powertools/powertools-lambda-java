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

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import software.amazon.lambda.powertools.common.internal.ClassPreLoader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.lambda.powertools.utilities.jmespath.Base64Function.decode;
import static software.amazon.lambda.powertools.utilities.jmespath.Base64GZipFunction.decompress;

import java.nio.ByteBuffer;

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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that can be used to extract the meaningful part of an event and deserialize it into a Java object.<br/>
 * For example, extract the body of an API Gateway event, or messages from an SQS event.
 */
public class EventDeserializer implements Resource{

    private static final Logger LOG = LoggerFactory.getLogger(EventDeserializer.class);
    private static final EventDeserializer INSTANCE = new EventDeserializer();
    public static final String DATA = "eyJpZCI6MTIzNCwgIm5hbWUiOiJwcm9kdWN0IiwgInByaWNlIjo0Mn0=";

    static {
        Core.getGlobalContext().register(INSTANCE);
    }

    public static void init() {
        // Placeholder method used to enable SnapStart priming. Users need a direct reference to this class in order for the CRaC hooks to execute.
        new EventDeserializer();
    }
    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        init();
        primeAllEventTypes();
        ClassPreLoader.preloadClasses();
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        // This is a no-op, as we don't need to do anything after restore
    }
    private void primeAllEventTypes() {
        try {
            // API Gateway v1
            APIGatewayProxyRequestEvent apiV1 = new APIGatewayProxyRequestEvent();
            apiV1.setBody("{}");
            extractDataFrom(apiV1);

            // API Gateway v2
            APIGatewayV2HTTPEvent apiV2 = new APIGatewayV2HTTPEvent();
            apiV2.setBody("{}");
            extractDataFrom(apiV2);

            // SNS
            SNSEvent snsEvent = new SNSEvent();
            SNSEvent.SNS sns = new SNSEvent.SNS();
            sns.setMessage("{}");
            SNSEvent.SNSRecord snsRecord = new SNSEvent.SNSRecord();
            snsRecord.setSns(sns);
            snsEvent.setRecords(List.of(snsRecord));
            extractDataFrom(snsEvent);

            // SQS
            SQSEvent sqsEvent = new SQSEvent();
            SQSEvent.SQSMessage sqsMessage = new SQSEvent.SQSMessage();
            sqsMessage.setBody("{}");
            sqsEvent.setRecords(List.of(sqsMessage));
            extractDataFrom(sqsEvent);

            // Single SQS message
            extractDataFrom(sqsMessage);

            // Scheduled Event
            ScheduledEvent scheduledEvent = new ScheduledEvent();
            scheduledEvent.setDetail(Map.of("key", "value"));
            extractDataFrom(scheduledEvent);

            // ALB
            ApplicationLoadBalancerRequestEvent albEvent =
                    new ApplicationLoadBalancerRequestEvent();
            albEvent.setBody("{}");
            extractDataFrom(albEvent);

            // CloudWatch Logs
            CloudWatchLogsEvent cwEvent = new CloudWatchLogsEvent();
            CloudWatchLogsEvent.AWSLogs logs = new CloudWatchLogsEvent.AWSLogs();
            logs.setData("ewogICJpZCI6IDEyMzQsCiAgIm5hbWUiOiAicHJvZHVjdCIsCiAgInByaWNlIjogNDIKfQ");
            cwEvent.setAwsLogs(logs);
            extractDataFrom(cwEvent);

            // CloudFormation
            CloudFormationCustomResourceEvent cfEvent =
                    new CloudFormationCustomResourceEvent();
            cfEvent.setResourceProperties(Map.of("key", "value"));
            extractDataFrom(cfEvent);

            // Kinesis
            KinesisEvent kinesisEvent = new KinesisEvent();
            KinesisEvent.Record kinesisRecord = new KinesisEvent.Record();
            kinesisRecord.setData(ByteBuffer.allocate(0));
            KinesisEvent.KinesisEventRecord kinesisEventRecord =
                    new KinesisEvent.KinesisEventRecord();
            kinesisEventRecord.setKinesis(kinesisRecord);
            kinesisEvent.setRecords(List.of(kinesisEventRecord));
            extractDataFrom(kinesisEvent);

            // Single Kinesis record
            extractDataFrom(kinesisEventRecord);

            // Firehose
            KinesisFirehoseEvent firehoseEvent = new KinesisFirehoseEvent();
            KinesisFirehoseEvent.Record firehoseRecord =
                    new KinesisFirehoseEvent.Record();
            firehoseRecord.setData(ByteBuffer.allocate(0));
            firehoseEvent.setRecords(List.of(firehoseRecord));
            extractDataFrom(firehoseEvent);

            // Kafka
            KafkaEvent kafkaEvent = new KafkaEvent();
            KafkaEvent.KafkaEventRecord kafkaRecord =
                    new KafkaEvent.KafkaEventRecord();
            kafkaRecord.setValue(DATA);
            kafkaEvent.setRecords(
                    Map.of("topic", List.of(kafkaRecord))
            );
            extractDataFrom(kafkaEvent);

            // ActiveMQ
            ActiveMQEvent activeMQEvent = new ActiveMQEvent();
            ActiveMQEvent.ActiveMQMessage activeMQMessage =
                    new ActiveMQEvent.ActiveMQMessage();
            activeMQMessage.setData(DATA);
            activeMQEvent.setMessages(List.of(activeMQMessage));
            extractDataFrom(activeMQEvent);

            // RabbitMQ
            RabbitMQEvent rabbitMQEvent = new RabbitMQEvent();
            RabbitMQEvent.RabbitMessage rabbitMessage =
                    new RabbitMQEvent.RabbitMessage();
            rabbitMessage.setData(DATA);
            rabbitMQEvent.setRmqMessagesByQueue(
                    Map.of("queue", List.of(rabbitMessage))
            );
            extractDataFrom(rabbitMQEvent);

            // Kinesis Analytics Firehose preprocessing
            KinesisAnalyticsFirehoseInputPreprocessingEvent kaFirehoseEvent =
                    new KinesisAnalyticsFirehoseInputPreprocessingEvent();
            KinesisAnalyticsFirehoseInputPreprocessingEvent.Record kaFirehoseRecord =
                    new KinesisAnalyticsFirehoseInputPreprocessingEvent.Record();
            kaFirehoseRecord.setData(ByteBuffer.allocate(0));
            kaFirehoseEvent.setRecords(List.of(kaFirehoseRecord));
            extractDataFrom(kaFirehoseEvent);

            // Kinesis Analytics Streams preprocessing
            KinesisAnalyticsStreamsInputPreprocessingEvent kaStreamsEvent =
                    new KinesisAnalyticsStreamsInputPreprocessingEvent();
            KinesisAnalyticsStreamsInputPreprocessingEvent.Record kaStreamsRecord =
                    new KinesisAnalyticsStreamsInputPreprocessingEvent.Record();
            kaStreamsRecord.setData(ByteBuffer.allocate(0));
            kaStreamsEvent.setRecords(List.of(kaStreamsRecord));
            extractDataFrom(kaStreamsEvent);

        } catch (Exception e) {
            // Best-effort priming only — never fail checkpointing
            throw new PrimingException("Failed to prime event ", e);
        }
    }

    /**
     * Extract the meaningful part of a Lambda Event object. Main events are built-in:
     * <ul>
     *     <li>{@link APIGatewayProxyRequestEvent}      -> body</li>
     *     <li>{@link APIGatewayV2HTTPEvent}            -> body</li>
     *     <li>{@link SNSEvent}                         -> Records[0].Sns.Message</li>
     *     <li>{@link SQSEvent}                         -> Records[*].body <i>(list)</i></li>
     *     <li>{@link ScheduledEvent}                   -> detail</li>
     *     <li>{@link ApplicationLoadBalancerRequestEvent} -> body</li>
     *     <li>{@link CloudWatchLogsEvent}              -> powertools_base64_gzip(data)</li>
     *     <li>{@link CloudFormationCustomResourceEvent} -> resourceProperties</li>
     *     <li>{@link KinesisEvent}                     -> Records[*].kinesis.powertools_base64(data) <i>(list)</i></li>
     *     <li>{@link KinesisFirehoseEvent}             -> Records[*].powertools_base64(data) <i>(list)</i></li>
     *     <li>{@link KafkaEvent}                       -> records[*].values[*].powertools_base64(value) <i>(list)</i></li>
     *     <li>{@link ActiveMQEvent}                    -> messages[*].powertools_base64(data) <i>(list)</i></li>
     *     <li>{@link RabbitMQEvent}                    -> rmqMessagesByQueue[*].values[*].powertools_base64(data) <i>(list)</i></li>
     *     <li>{@link KinesisAnalyticsFirehoseInputPreprocessingEvent} -> Records[*].kinesis.powertools_base64(data) <i>(list)</i></li>
     *     <li>{@link KinesisAnalyticsStreamsInputPreprocessingEvent} > Records[*].kinesis.powertools_base64(data) <i>(list)</i></li>
     *     <li>{@link String}</li>
     *     <li>{@link Map}</li>
     * </ul>
     * To be used in conjunction with {@link EventPart#as(Class)} or {@link EventPart#asListOf(Class)}
     * for the deserialization.
     *
     * @param object the event of your Lambda function handler method
     * @return the part of the event which is meaningful (ex: body of the API Gateway).<br/>
     */
    public static EventPart extractDataFrom(Object object) {
        if (object instanceof String) {
            return new EventPart((String) object);
        } else if (object instanceof Map) {
            return new EventPart((Map<String, Object>) object);
        } else if (object instanceof APIGatewayProxyRequestEvent) {
            APIGatewayProxyRequestEvent event = (APIGatewayProxyRequestEvent) object;
            return new EventPart(event.getBody());
        } else if (object instanceof APIGatewayV2HTTPEvent) {
            APIGatewayV2HTTPEvent event = (APIGatewayV2HTTPEvent) object;
            return new EventPart(event.getBody());
        } else if (object instanceof SNSEvent) {
            SNSEvent event = (SNSEvent) object;
            return new EventPart(event.getRecords().get(0).getSNS().getMessage());
        } else if (object instanceof SQSEvent) {
            SQSEvent event = (SQSEvent) object;
            return new EventPart(event.getRecords().stream()
                    .map(SQSEvent.SQSMessage::getBody)
                    .collect(Collectors.toList()));
        } else if (object instanceof SQSEvent.SQSMessage) {
            return new EventPart(((SQSEvent.SQSMessage) object).getBody());
        } else if (object instanceof ScheduledEvent) {
            ScheduledEvent event = (ScheduledEvent) object;
            return new EventPart(event.getDetail());
        } else if (object instanceof ApplicationLoadBalancerRequestEvent) {
            ApplicationLoadBalancerRequestEvent event = (ApplicationLoadBalancerRequestEvent) object;
            return new EventPart(event.getBody());
        } else if (object instanceof CloudWatchLogsEvent) {
            CloudWatchLogsEvent event = (CloudWatchLogsEvent) object;
            return new EventPart(decompress(decode(event.getAwsLogs().getData().getBytes(UTF_8))));
        } else if (object instanceof CloudFormationCustomResourceEvent) {
            CloudFormationCustomResourceEvent event = (CloudFormationCustomResourceEvent) object;
            return new EventPart(event.getResourceProperties());
        } else if (object instanceof KinesisEvent) {
            KinesisEvent event = (KinesisEvent) object;
            return new EventPart(event.getRecords().stream()
                    .map(r -> decode(r.getKinesis().getData()))
                    .collect(Collectors.toList()));
        } else if (object instanceof KinesisEvent.KinesisEventRecord) {
            return new EventPart(decode(((KinesisEvent.KinesisEventRecord)object).getKinesis().getData()));
        } else if (object instanceof KinesisFirehoseEvent) {
            KinesisFirehoseEvent event = (KinesisFirehoseEvent) object;
            return new EventPart(event.getRecords().stream()
                    .map(r -> decode(r.getData()))
                    .collect(Collectors.toList()));
        } else if (object instanceof KafkaEvent) {
            KafkaEvent event = (KafkaEvent) object;
            return new EventPart(event.getRecords().values().stream()
                    .flatMap(List::stream)
                    .map(r -> decode(r.getValue()))
                    .collect(Collectors.toList()));
        } else if (object instanceof ActiveMQEvent) {
            ActiveMQEvent event = (ActiveMQEvent) object;
            return new EventPart(event.getMessages().stream()
                    .map(m -> decode(m.getData()))
                    .collect(Collectors.toList()));
        } else if (object instanceof RabbitMQEvent) {
            RabbitMQEvent event = (RabbitMQEvent) object;
            return new EventPart(event.getRmqMessagesByQueue().values().stream()
                    .flatMap(List::stream)
                    .map(r -> decode(r.getData()))
                    .collect(Collectors.toList()));
        } else if (object instanceof KinesisAnalyticsFirehoseInputPreprocessingEvent) {
            KinesisAnalyticsFirehoseInputPreprocessingEvent event =
                    (KinesisAnalyticsFirehoseInputPreprocessingEvent) object;
            return new EventPart(event.getRecords().stream()
                    .map(r -> decode(r.getData()))
                    .collect(Collectors.toList()));
        } else if (object instanceof KinesisAnalyticsStreamsInputPreprocessingEvent) {
            KinesisAnalyticsStreamsInputPreprocessingEvent event =
                    (KinesisAnalyticsStreamsInputPreprocessingEvent) object;
            return new EventPart(event.getRecords().stream()
                    .map(r -> decode(r.getData()))
                    .collect(Collectors.toList()));
        } else {
            // does not really make sense to use this EventDeserializer when you already have a typed object
            // just not to throw an exception
            LOG.warn("Consider using your object directly instead of using EventDeserializer");
            return new EventPart(object);
        }
    }

    /**
     * Meaningful part of a Lambda event.<br/>
     * Use {@link #extractDataFrom(Object)} to retrieve an instance of this class.
     */
    public static class EventPart {
        private Map<String, Object> contentMap;
        private String content;
        private List<String> contentList;
        private Object contentObject;

        private EventPart(List<String> contentList) {
            this.contentList = contentList;
        }

        private EventPart(String content) {
            this.content = content;
        }

        private EventPart(Map<String, Object> contentMap) {
            this.contentMap = contentMap;
        }

        private EventPart(Object content) {
            this.contentObject = content;
        }

        /**
         * Deserialize this part of event from JSON to an object of type T
         *
         * @param clazz the target type for deserialization
         * @param <T>   type of object to return
         * @return an Object of type T (deserialized from the content)
         */
        public <T> T as(Class<T> clazz) {
            try {
                if (content != null) {
                    if (content.getClass().equals(clazz)) {
                        // do not read json when returning String, just return the String
                        return (T) content;
                    }
                    return JsonConfig.get().getObjectMapper().reader().readValue(content, clazz);
                }
                if (contentMap != null) {
                    return JsonConfig.get().getObjectMapper().convertValue(contentMap, clazz);
                }
                if (contentObject != null) {
                    return (T) contentObject;
                }
                if (contentList != null) {
                    throw new EventDeserializationException(
                            "The content of this event is a list, consider using 'asListOf' instead");
                }
                // should not occur, except if the event is malformed (missing fields)
                throw new IllegalStateException("Event content is null: the event may be malformed (missing fields)");
            } catch (IOException e) {
                throw new EventDeserializationException("Cannot load the event as " + clazz.getSimpleName(), e);
            }
        }

        public <M> M as() {
            TypeReference<M> typeRef = new TypeReference<M>() {};

            try {
                JsonParser parser = JsonConfig.get().getObjectMapper().createParser(content);
                return JsonConfig.get().getObjectMapper().reader().readValue(parser, typeRef);
            } catch (IOException e) {
                throw new EventDeserializationException("Cannot load the event as " + typeRef, e);
            }
        };

        /**
         * Deserialize this part of event from JSON to a list of objects of type T
         *
         * @param clazz the target type for deserialization
         * @param <T>   type of object to return
         * @return a list of objects of type T (deserialized from the content)
         */
        public <T> List<T> asListOf(Class<T> clazz) {
            if (contentList == null && content == null) {
                if (contentMap != null || contentObject != null) {
                    throw new EventDeserializationException(
                            "The content of this event is not a list, consider using 'as' instead");
                }
                // should not occur, except if the event is really malformed
                throw new IllegalStateException("Event content is null: the event may be malformed (missing fields)");
            }
            if (content != null) {
                ObjectReader reader = JsonConfig.get().getObjectMapper().readerForListOf(clazz);
                try {
                    return reader.readValue(content);
                } catch (JsonProcessingException e) {
                    throw new EventDeserializationException(
                            "Cannot load the event as a list of " + clazz.getSimpleName() +
                                    ", consider using 'as' instead", e);
                }
            } else {
                return contentList.stream().map(s ->
                {
                    try {
                        return s == null ? null : JsonConfig.get().getObjectMapper().reader().readValue(s, clazz);
                    } catch (IOException e) {
                        throw new EventDeserializationException(
                                "Cannot load the event as a list of " + clazz.getSimpleName(), e);
                    }
                }).collect(Collectors.toList());
            }
        }
    }
}
