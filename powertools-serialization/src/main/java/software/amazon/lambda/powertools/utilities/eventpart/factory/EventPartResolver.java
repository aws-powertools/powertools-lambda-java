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

import software.amazon.lambda.powertools.utilities.EventDeserializer;

/**
 * Interface implemented by the event resolvers.
 * Different type of event resolvers can implement it to retrieve the {@link software.amazon.lambda.powertools.utilities.EventDeserializer.EventPart}
 */
public interface EventPartResolver {

    /**
     * Extract the {@link software.amazon.lambda.powertools.utilities.EventDeserializer.EventPart} from the {@param event}
     *
     * @param event the event of the Lambda function handler method
     * @return the {@link software.amazon.lambda.powertools.utilities.EventDeserializer.EventPart} which contains the event data (e.g. body)
     */
    EventDeserializer.EventPart createEventPart(Object event);
}
