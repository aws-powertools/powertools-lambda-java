/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.
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

import com.amazonaws.services.lambda.runtime.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.amazon.lambda.powertools.utilities.jmespath.Base64Function.decode;
import static software.amazon.lambda.powertools.utilities.jmespath.Base64GZipFunction.decompress;

/**
 * Class that can be used to extract the meaningful part of an event and deserialize it into a Java object.<br/>
 * For example, extract the body of an API Gateway event, or messages from an SQS event.
 */
public class EventDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(EventDeserializer.class);

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
            KinesisAnalyticsFirehoseInputPreprocessingEvent event = (KinesisAnalyticsFirehoseInputPreprocessingEvent) object;
            return new EventPart(event.getRecords().stream()
                    .map(r -> decode(r.getData()))
                    .collect(Collectors.toList()));
        } else if (object instanceof KinesisAnalyticsStreamsInputPreprocessingEvent) {
            KinesisAnalyticsStreamsInputPreprocessingEvent event = (KinesisAnalyticsStreamsInputPreprocessingEvent) object;
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
         * @param clazz the target type for deserialization
         * @param <T> type of object to return
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
                    throw new EventDeserializationException("The content of this event is a list, consider using 'asListOf' instead");
                }
                // should not occur, except if the event is malformed (missing fields)
                throw new IllegalStateException("Event content is null: the event may be malformed (missing fields)");
            } catch (IOException e) {
                throw new EventDeserializationException("Cannot load the event as " + clazz.getSimpleName(), e);
            }
        }

        /**
         * Deserialize this part of event from JSON to a list of objects of type T
         * @param clazz the target type for deserialization
         * @param <T> type of object to return
         * @return a list of objects of type T (deserialized from the content)
         */
        public <T> List<T> asListOf(Class<T> clazz) {
            if (contentList == null) {
                if (content != null || contentMap != null || contentObject != null) {
                    throw new EventDeserializationException("The content of this event is not a list, consider using 'as' instead");
                }
                // should not occur, except if the event is really malformed
                throw new IllegalStateException("Event content is null: the event may be malformed (missing fields)");
            }
            return contentList.stream().map(s -> {
                try {
                    return s == null ? null : JsonConfig.get().getObjectMapper().reader().readValue(s, clazz);
                } catch (IOException e) {
                    throw new EventDeserializationException("Cannot load the event as " + clazz.getSimpleName(), e);
                }
            }).collect(Collectors.toList());
        }
    }
}
