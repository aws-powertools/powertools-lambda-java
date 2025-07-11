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

package org.demo.serialization;

import static software.amazon.lambda.powertools.utilities.EventDeserializer.extractDataFrom;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SQSEventDeserializationFunction implements RequestHandler<SQSEvent, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQSEventDeserializationFunction.class);

    public String handleRequest(SQSEvent event, Context context) {
        List<Product> products = extractDataFrom(event).asListOf(Product.class);

        LOGGER.info("\n=============== Deserialized messages: ===============");
        LOGGER.info("products={}\n", products);

        return "Number of received messages: " + products.size();
    }
}

