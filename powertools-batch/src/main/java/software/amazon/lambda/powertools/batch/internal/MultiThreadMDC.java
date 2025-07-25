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

package software.amazon.lambda.powertools.batch.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * MDC (SLF4J) is not passed to other threads (ThreadLocal).
 * This class permits to manually copy the MDC to a given thread.
 */
public class MultiThreadMDC {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiThreadMDC.class);

    private final List<String> mdcAwareThreads = new ArrayList<>();
    private final Map<String, String> contextMap;
    
    public MultiThreadMDC() {
        mdcAwareThreads.add("main");
        contextMap = MDC.getCopyOfContextMap();
    }
    
    public void copyMDCToThread(String thread) {
        if (!mdcAwareThreads.contains(thread)) {
            LOGGER.debug("Copy MDC to thread {}", thread);
            MDC.setContextMap(contextMap);
            mdcAwareThreads.add(thread);
        }
    }

    public void removeThread(String thread) {
        if (mdcAwareThreads.contains(thread)) {
            LOGGER.debug("Removing thread {}", thread);
            mdcAwareThreads.remove(thread);
        }
    }
}
