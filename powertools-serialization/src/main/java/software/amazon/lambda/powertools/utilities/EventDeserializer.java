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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.utilities.eventpart.factory.EventPartResolver;
import software.amazon.lambda.powertools.utilities.eventpart.factory.EventPartResolverFactory;
import software.amazon.lambda.powertools.utilities.eventpart.resolvers.APIGatewayProxyRequestEventPartResolver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static software.amazon.lambda.powertools.utilities.jmespath.Base64Function.decode;

/**
 * Class that can be used to extract the meaningful part of an event and deserialize it into a Java object.<br/>
 * For example, extract the body of an API Gateway event, or messages from an SQS event.
 */
public class EventDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(EventDeserializer.class);

    /**
     * To be used in conjunction with {@link EventPart#as(Class)} or {@link EventPart#asListOf(Class)}
     * for the deserialization.
     *
     * @param event the event of your Lambda function handler method
     * @return the part of the event which is meaningful (ex: body of the API Gateway).<br/>
     */
    public static EventPart extractDataFrom(Object event) {
            EventPartResolverFactory factory = new EventPartResolverFactory();
            EventPartResolver generator = factory.resolveEventType(event);
            return generator.createEventPart(event);
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
            if (contentList == null && content == null) {
                if (contentMap != null || contentObject != null) {
                    throw new EventDeserializationException("The content of this event is not a list, consider using 'as' instead");
                }
                // should not occur, except if the event is really malformed
                throw new IllegalStateException("Event content is null: the event may be malformed (missing fields)");
            }
            if (content != null) {
                ObjectReader reader = JsonConfig.get().getObjectMapper().readerForListOf(clazz);
                try {
                    return reader.readValue(content);
                } catch (JsonProcessingException e) {
                    throw new EventDeserializationException("Cannot load the event as a list of " + clazz.getSimpleName() + ", consider using 'as' instead", e);
                }
            } else {
                return contentList.stream().map(s -> {
                    try {
                        return s == null ? null : JsonConfig.get().getObjectMapper().reader().readValue(s, clazz);
                    } catch (IOException e) {
                        throw new EventDeserializationException("Cannot load the event as a list of " + clazz.getSimpleName(), e);
                    }
                }).collect(Collectors.toList());
            }
        }
    }
}
