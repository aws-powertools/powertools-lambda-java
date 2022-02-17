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

public class EventDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(EventDeserializer.class);

    public static EventPart from(Object obj) {
        if (obj instanceof String) {
            return new EventPart((String) obj);
        } else if (obj instanceof Map) {
            return new EventPart((Map<String, Object>) obj);
        } else if (obj instanceof APIGatewayProxyRequestEvent) {
            APIGatewayProxyRequestEvent event = (APIGatewayProxyRequestEvent) obj;
            return new EventPart(event.getBody());
        } else if (obj instanceof APIGatewayV2HTTPEvent) {
            APIGatewayV2HTTPEvent event = (APIGatewayV2HTTPEvent) obj;
            return new EventPart(event.getBody());
        } else if (obj instanceof SNSEvent) {
            SNSEvent event = (SNSEvent) obj;
            return new EventPart(event.getRecords().get(0).getSNS().getMessage());
        } else if (obj instanceof SQSEvent) {
            SQSEvent event = (SQSEvent) obj;
            return new EventPart(event.getRecords().stream().map(SQSEvent.SQSMessage::getBody).collect(Collectors.toList()));
        } else if (obj instanceof ScheduledEvent) {
            ScheduledEvent event = (ScheduledEvent) obj;
            return new EventPart(event.getDetail());
        } else if (obj instanceof ApplicationLoadBalancerRequestEvent) {
            ApplicationLoadBalancerRequestEvent event = (ApplicationLoadBalancerRequestEvent) obj;
            return new EventPart(event.getBody());
        } else if (obj instanceof CloudWatchLogsEvent) {
            CloudWatchLogsEvent event = (CloudWatchLogsEvent) obj;
            return new EventPart(decompress(decode(event.getAwsLogs().getData().getBytes(UTF_8))));
        } else if (obj instanceof CloudFormationCustomResourceEvent) {
            CloudFormationCustomResourceEvent event = (CloudFormationCustomResourceEvent) obj;
            return new EventPart(event.getResourceProperties());
        } else if (obj instanceof KinesisEvent) {
            KinesisEvent event = (KinesisEvent) obj;
            return new EventPart(event.getRecords().stream().map(r -> decode(r.getKinesis().getData())).collect(Collectors.toList()));
        } else if (obj instanceof KinesisFirehoseEvent) {
            KinesisFirehoseEvent event = (KinesisFirehoseEvent) obj;
            return new EventPart(event.getRecords().stream().map(r -> decode(r.getData())).collect(Collectors.toList()));
        } else if (obj instanceof KafkaEvent) {
            KafkaEvent event = (KafkaEvent) obj;
            return new EventPart(event.getRecords().values().stream().flatMap(List::stream).map(r -> decode(r.getValue())).collect(Collectors.toList()));
        } else if (obj instanceof ActiveMQEvent) {
            ActiveMQEvent event = (ActiveMQEvent) obj;
            return new EventPart(event.getMessages().stream().map(m -> decode(m.getData())).collect(Collectors.toList()));
        } else if (obj instanceof RabbitMQEvent) {
            RabbitMQEvent event = (RabbitMQEvent) obj;
            return new EventPart(event.getRmqMessagesByQueue().values().stream().flatMap(List::stream).map(r -> decode(r.getData())).collect(Collectors.toList()));
        } else if (obj instanceof KinesisAnalyticsFirehoseInputPreprocessingEvent) {
            KinesisAnalyticsFirehoseInputPreprocessingEvent event = (KinesisAnalyticsFirehoseInputPreprocessingEvent) obj;
            return new EventPart(event.getRecords().stream().map(r -> decode(r.getData())).collect(Collectors.toList()));
        } else if (obj instanceof KinesisAnalyticsStreamsInputPreprocessingEvent) {
            KinesisAnalyticsStreamsInputPreprocessingEvent event = (KinesisAnalyticsStreamsInputPreprocessingEvent) obj;
            return new EventPart(event.getRecords().stream().map(r -> decode(r.getData())).collect(Collectors.toList()));
        } else {
            // does not really make sense to use this EventLoader when you already have a typed object
            // just not to throw an exception
            LOG.warn("Consider using your object directly instead of using EventDeserializer");
            return new EventPart(obj);
        }
    }

    public static class EventPart {
        private Map<String, Object> contentMap;
        private String content;
        private List<String> contentList;
        private Object contentObject;

        public EventPart(List<String> contentList) {
            this.contentList = contentList;
        }

        public EventPart(String content) {
            this.content = content;
        }

        public EventPart(Map<String, Object> contentMap) {
            this.contentMap = contentMap;
        }

        public EventPart(Object content) {
            this.contentObject = content;
        }

        public <T> T extractDataAs(Class<T> clazz) {
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
                    throw new EventDeserializationException("The content of this event is a list, consider using 'extractDataAsListOf' instead");
                }
                throw new EventDeserializationException("Event content is null");
            } catch (IOException e) {
                throw new EventDeserializationException("Cannot load the event as " + clazz.getSimpleName(), e);
            }
        }

        public <T> List<T> extractDataAsListOf(Class<T> clazz) {
            if (contentList == null) {
                throw new EventDeserializationException("Event content is null");
            }
            return contentList.stream().map(s -> {
                try {
                    return JsonConfig.get().getObjectMapper().reader().readValue(s, clazz);
                } catch (IOException e) {
                    throw new EventDeserializationException("Cannot load the event as " + clazz.getSimpleName(), e);
                }
            }).collect(Collectors.toList());
        }
    }
}
