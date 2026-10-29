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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * MDC (SLF4J) is not passed to other threads (ThreadLocal).
 * This class permits to manually copy the MDC to a given thread.
 */
public class MultiThreadMDC {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiThreadMDC.class);

    private final Set<String> mdcAwareThreads = ConcurrentHashMap.newKeySet();
    private final Map<String, String> contextMap;
    
    public MultiThreadMDC() {
        mdcAwareThreads.add("main");
        contextMap = MDC.getCopyOfContextMap();
    }
    
    public boolean copyMDCToThread(String thread) {
        if (mdcAwareThreads.add(thread)) {
            LOGGER.debug("Copy MDC to thread {}", thread);
            MDC.setContextMap(contextMap);
            return true;
        }
        return false;
    }

    public boolean removeThread(String thread) {
        if (mdcAwareThreads.remove(thread)) {
            LOGGER.debug("Removed thread {}", thread);
            return true;
        }
        return false;
    }
}
