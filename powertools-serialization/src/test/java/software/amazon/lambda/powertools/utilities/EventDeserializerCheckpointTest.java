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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventDeserializerCheckpointTest {

    private final EventDeserializer eventDeserializer = new EventDeserializer();

    @Test
    void init_shouldNotThrow() {
        assertThatNoException().isThrownBy(EventDeserializer::init);
    }

    @Test
    void beforeCheckpoint_shouldNotThrow() {
        assertThatNoException().isThrownBy(() -> eventDeserializer.beforeCheckpoint(null));
    }

    @Test
    void afterRestore_shouldNotThrow() {
        assertThatNoException().isThrownBy(() -> eventDeserializer.afterRestore(null));
    }

    @Test
    void prime_shouldDeserializeAllSupportedEventTypesWithJackson() {
        Set<Class<?>> primed = EventDeserializerPriming.prime();

        assertThat(primed).containsExactlyInAnyOrder(
                String.class,
                Map.class,
                APIGatewayProxyRequestEvent.class,
                APIGatewayV2HTTPEvent.class,
                SNSEvent.class,
                SQSEvent.class,
                SQSEvent.SQSMessage.class,
                ScheduledEvent.class,
                ApplicationLoadBalancerRequestEvent.class,
                CloudWatchLogsEvent.class,
                CloudFormationCustomResourceEvent.class,
                KinesisEvent.class,
                KinesisEvent.KinesisEventRecord.class,
                KinesisFirehoseEvent.class,
                KafkaEvent.class,
                ActiveMQEvent.class,
                RabbitMQEvent.class,
                KinesisAnalyticsFirehoseInputPreprocessingEvent.class,
                KinesisAnalyticsStreamsInputPreprocessingEvent.class);

        Map<String, Object> payload = extractDataFrom(EventDeserializerPriming.SAMPLE_JSON).as(Map.class);
        assertThat(payload)
                .containsEntry("id", 1234)
                .containsEntry("name", "product")
                .containsEntry("price", 42);
    }
}
