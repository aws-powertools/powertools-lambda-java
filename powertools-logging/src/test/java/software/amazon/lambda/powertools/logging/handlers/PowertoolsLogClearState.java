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

package software.amazon.lambda.powertools.logging.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import software.amazon.lambda.powertools.logging.Logging;

public class PowertoolsLogClearState implements RequestHandler<Map<String, String>, Object> {
    private final Logger LOG = LoggerFactory.getLogger(PowertoolsLogClearState.class);

    @Override
    @Logging(clearState = true)
    public Object handleRequest(Map<String, String> input, Context context) {
        MDC.put("mySuperSecret", input.get("mySuperSecret"));
        LOG.info("Test event");
        return null;
    }
}
