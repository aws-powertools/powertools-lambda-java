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

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to propagate X-Ray trace entity context to worker threads using reflection.
 * Reflection is used to avoid taking a dependency on X-RAY SDK.
 */
public final class XRayTraceEntityPropagator {
    private static final Logger LOGGER = LoggerFactory.getLogger(XRayTraceEntityPropagator.class);
    private static final boolean XRAY_AVAILABLE;
    private static final Method GET_TRACE_ENTITY_METHOD;

    // We do the more "expensive" Class.forName in this static block to detect exactly once at import time if X-RAY
    // is available or not. Subsequent <method>.invoke() are very fast on modern JDKs.
    static {
        Method method = null;
        boolean available = false;

        try {
            Class<?> awsXRayClass = Class.forName("com.amazonaws.xray.AWSXRay");
            method = awsXRayClass.getMethod("getTraceEntity");
            available = true;
            LOGGER.debug("X-Ray SDK detected. Trace context will be propagated to worker threads.");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            LOGGER.debug("X-Ray SDK not detected. Trace context propagation disabled");
        }

        GET_TRACE_ENTITY_METHOD = method;
        XRAY_AVAILABLE = available;
    }

    private XRayTraceEntityPropagator() {
        // Utility class
    }

    public static Object captureTraceEntity() {
        if (!XRAY_AVAILABLE) {
            return null;
        }

        try {
            return GET_TRACE_ENTITY_METHOD.invoke(null);
        } catch (Exception e) {
            // We don't want to break batch processing if this fails.
            LOGGER.warn("Failed to capture trace entity.", e);
            return null;
        }
    }

    // See https://docs.aws.amazon.com/xray/latest/devguide/scorekeep-workerthreads.html
    public static void runWithEntity(Object traceEntity, Runnable runnable) {
        if (!XRAY_AVAILABLE || traceEntity == null) {
            runnable.run();
            return;
        }

        try {
            traceEntity.getClass().getMethod("run", Runnable.class).invoke(traceEntity, runnable);
        } catch (Exception e) {
            // We don't want to break batch processing if this fails.
            LOGGER.warn("Failed to run with trace entity, falling back to direct execution.", e);
            runnable.run();
        }
    }
}
