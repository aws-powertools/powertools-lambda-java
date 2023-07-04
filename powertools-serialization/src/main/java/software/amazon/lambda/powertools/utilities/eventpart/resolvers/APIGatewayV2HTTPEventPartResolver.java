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

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import software.amazon.lambda.powertools.utilities.EventDeserializer;
import software.amazon.lambda.powertools.utilities.eventpart.factory.EventPartResolver;

/**
 * Implements the {@link EventPartResolver} to retrieve the {@link software.amazon.lambda.powertools.utilities.EventDeserializer.EventPart}
 * for events of type {@link APIGatewayV2HTTPEvent}
 */
public class APIGatewayV2HTTPEventPartResolver implements EventPartResolver {
    @Override
    public EventDeserializer.EventPart createEventPart(Object event) {
        APIGatewayV2HTTPEvent apiGatewayV2HTTPEvent = (APIGatewayV2HTTPEvent) event;
        return new EventDeserializer.EventPart(apiGatewayV2HTTPEvent.getBody());
    }
}
