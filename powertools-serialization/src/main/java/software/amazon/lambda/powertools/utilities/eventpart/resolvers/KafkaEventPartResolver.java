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
package software.amazon.lambda.powertools.utilities.eventpart.resolvers;

import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import software.amazon.lambda.powertools.utilities.EventDeserializer;
import software.amazon.lambda.powertools.utilities.eventpart.factory.EventPartResolver;

import java.util.List;
import java.util.stream.Collectors;

import static software.amazon.lambda.powertools.utilities.jmespath.Base64Function.decode;

/**
 * Implements the {@link EventPartResolver} to retrieve the {@link software.amazon.lambda.powertools.utilities.EventDeserializer.EventPart}
 * for events of type {@link KafkaEvent}
 */
public class KafkaEventPartResolver implements EventPartResolver {
    @Override
    public EventDeserializer.EventPart createEventPart(Object event) {
        KafkaEvent kafkaEvent = (KafkaEvent) event;
        return new EventDeserializer.EventPart(kafkaEvent.getRecords().values().stream()
                .flatMap(List::stream)
                .map(r -> decode(r.getValue()))
                .collect(Collectors.toList()));
    }
}
