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
package software.amazon.lambda.powertools.utilities.eventpart.factory;

import com.amazonaws.services.lambda.runtime.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.utilities.eventpart.resolvers.*;

import java.util.Map;

/**
 * Factory Class that identifies the event type and invokes the appropriate implementation of an {@link EventPartResolver}
 * <p>
 * Main events are built-in:
 * <ul>
 *     <li>{@link APIGatewayProxyRequestEventPartResolver}      -> body</li>
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
 */
public class EventPartResolverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(EventPartResolverFactory.class);

    public EventPartResolver resolveEventType(Object eventType) {

        if (eventType instanceof APIGatewayProxyRequestEvent) {
            return new APIGatewayProxyRequestEventPartResolver();
        } else if (eventType instanceof String) {
            return new StringEventPartResolver();
        } else if (eventType instanceof Map) {
            return new MapEventPartResolver();
        } else if (eventType instanceof APIGatewayV2HTTPEvent) {
            return new APIGatewayV2HTTPEventPartResolver();
        } else if (eventType instanceof SNSEvent) {
            return new SNSEventPartResolver();
        } else if (eventType instanceof SQSEvent) {
            return new SQSEventPartResolver();
        } else if (eventType instanceof ScheduledEvent) {
            return new ScheduledEventPartResolver();
        } else if (eventType instanceof ApplicationLoadBalancerRequestEvent) {
            return new ApplicationLoadBalancerRequestEventPartResolver();
        } else if (eventType instanceof CloudWatchLogsEvent) {
            return new CloudWatchLogsEventPartResolver();
        } else if (eventType instanceof CloudFormationCustomResourceEvent) {
            return new CloudFormationCustomResourceEventPartResolver();
        } else if (eventType instanceof KinesisEvent) {
            return new KinesisEventPartResolver();
        } else if (eventType instanceof KinesisFirehoseEvent) {
            return new KinesisFirehoseEventPartResolver();
        } else if (eventType instanceof KafkaEvent) {
            return new KafkaEventPartResolver();
        } else if (eventType instanceof ActiveMQEvent) {
            return new ActiveMQEventPartResolver();
        } else if (eventType instanceof RabbitMQEvent) {
            return new RabbitMQEventPartResolver();
        } else if (eventType instanceof KinesisAnalyticsFirehoseInputPreprocessingEvent) {
            return new KinesisAnalyticsFirehoseInputPreprocessingEventPartResolver();
        } else if (eventType instanceof KinesisAnalyticsStreamsInputPreprocessingEvent) {
            return new KinesisAnalyticsStreamsInputPreprocessingEventPartResolver();
        } else {
            // does not really make sense to use this EventDeserializer when you already have a typed object
            // this is used to avoid throwing an exception
            LOG.warn("Consider using your object directly instead of using EventDeserializer");
            return new GenericEventPartResolver();
        }
    }

}
