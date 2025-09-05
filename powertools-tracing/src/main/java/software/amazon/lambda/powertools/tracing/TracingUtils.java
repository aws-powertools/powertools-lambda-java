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

package software.amazon.lambda.powertools.tracing;

import static software.amazon.lambda.powertools.common.internal.LambdaHandlerProcessor.serviceName;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Subsegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.lambda.powertools.common.internal.ClassPreLoader;

/**
 * A class of helper functions to add additional functionality and ease
 * of use.
 */
public final class TracingUtils implements Resource {
    private static final Logger LOG = LoggerFactory.getLogger(TracingUtils.class);
    private static ObjectMapper objectMapper;

    // Static instance for CRaC Resource registration (same pattern as MetricsFactory)
    private static final TracingUtils INSTANCE = new TracingUtils();

    // Static block to ensure CRaC registration happens at class loading time
    static {
        Core.getGlobalContext().register(INSTANCE);
    }

    private TracingUtils() {
        // Private constructor for singleton pattern
    }

    /**
     * Put an annotation to the current subsegment with a String value.
     *
     * @param key   the key of the annotation
     * @param value the value of the annotation
     */
    public static void putAnnotation(String key, String value) {
        if (!isValidAnnotationKey(key)) {
            LOG.warn("Ignoring annotation with unsupported characters in key: {}", key);
            return;
        }
        AWSXRay.getCurrentSubsegmentOptional()
                .ifPresent(segment -> segment.putAnnotation(key, value));
    }

    /**
     * Put an annotation to the current subsegment with a Number value.
     *
     * @param key   the key of the annotation
     * @param value the value of the annotation
     */
    public static void putAnnotation(String key, Number value) {
        if (!isValidAnnotationKey(key)) {
            LOG.warn("Ignoring annotation with unsupported characters in key: {}", key);
            return;
        }
        AWSXRay.getCurrentSubsegmentOptional()
                .ifPresent(segment -> segment.putAnnotation(key, value));
    }

    /**
     Make sure that the annotation key is valid according to
     <a href='https://docs.aws.amazon.com/xray/latest/devguide/xray-api-segmentdocuments.html#api-segmentdocuments-annotations'>the documentation</a>.

     Annotation keys that are added that are invalid are ignored by x-ray.
     **/
    private static boolean isValidAnnotationKey(String key) {
        return key.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * Put an annotation to the current subsegment with a Boolean value.
     *
     * @param key   the key of the annotation
     * @param value the value of the annotation
     */
    public static void putAnnotation(String key, Boolean value) {
        AWSXRay.getCurrentSubsegmentOptional()
                .ifPresent(segment -> segment.putAnnotation(key, value));
    }

    /**
     * Put additional metadata for the current subsegment.
     * <p>
     * The namespace used will be the namespace of the current subsegment if it
     * is set else it will follow the namespace process as described in
     * {@link Tracing}
     *
     * @param key   the key of the metadata
     * @param value the value of the metadata
     */
    public static void putMetadata(String key, Object value) {
        String namespace = AWSXRay.getCurrentSubsegmentOptional()
                .map(Subsegment::getNamespace).orElse(serviceName());

        putMetadata(namespace, key, value);
    }

    /**
     * Put additional metadata for the current subsegment.
     *
     * @param namespace the namespace of the metadata
     * @param key       the key of the metadata
     * @param value     the value of the metadata
     */
    public static void putMetadata(String namespace, String key, Object value) {
        AWSXRay.getCurrentSubsegmentOptional()
                .ifPresent(segment -> segment.putMetadata(namespace, key, value));
    }

    /**
     * Adds a new subsegment around the passed consumer. This also provides access to
     * the newly created subsegment.
     * <p>
     * The namespace used follows the flow as described in {@link Tracing}
     * <p>
     * This method is intended for use with multi-threaded programming where the
     * context is lost between threads.
     *
     * @param name       the name of the subsegment
     * @param entity     the current x-ray context
     * @param subsegment the x-ray subsegment for the wrapped consumer
     */
    public static void withEntitySubsegment(String name, Entity entity, Consumer<Subsegment> subsegment) {
        AWSXRay.setTraceEntity(entity);
        withEntitySubsegment(serviceName(), name, entity, subsegment);
    }

    /**
     * Adds a new subsegment around the passed consumer. This also provides access to
     * the newly created subsegment.
     * <p>
     * This method is intended for use with multi-threaded programming where the
     * context is lost between threads.
     *
     * @param namespace  the namespace of the subsegment
     * @param name       the name of the subsegment
     * @param entity     the current x-ray context
     * @param subsegment the x-ray subsegment for the wrapped consumer
     */
    public static void withEntitySubsegment(String namespace, String name, Entity entity,
                                            Consumer<Subsegment> subsegment) {
        AWSXRay.setTraceEntity(entity);
        withSubsegment(namespace, name, subsegment);
    }

    /**
     * Adds a new subsegment around the passed consumer. This also provides access to
     * the newly created subsegment.
     * <p>
     * The namespace used follows the flow as described in {@link Tracing}
     *
     * @param name       the name of the subsegment
     * @param subsegment the x-ray subsegment for the wrapped consumer
     */
    public static void withSubsegment(String name, Consumer<Subsegment> subsegment) {
        withSubsegment(serviceName(), name, subsegment);
    }

    /**
     * Adds a new subsegment around the passed consumer. This also provides access to
     * the newly created subsegment.
     *
     * @param namespace  the namespace for the subsegment
     * @param name       the name of the subsegment
     * @param subsegment the x-ray subsegment for the wrapped consumer
     */
    public static void withSubsegment(String namespace, String name, Consumer<Subsegment> subsegment) {
        Subsegment segment = AWSXRay.beginSubsegment("## " + name);
        segment.setNamespace(namespace);
        try {
            subsegment.accept(segment);
        } finally {
            AWSXRay.endSubsegment();
        }
    }

    /**
     * Sets the instance of ObjectMapper object which is used for serialising object response when
     * {@code @Tracing(captureMode=CaptureMode.RESPONSE)}.
     *
     * @param objectMapper Custom implementation of object mapper to be used for serializing response
     */
    public static void defaultObjectMapper(ObjectMapper objectMapper) {
        TracingUtils.objectMapper = objectMapper;
    }

    public static ObjectMapper objectMapper() {
        return objectMapper;
    }

    /**
     * Prime TracingUtils for AWS Lambda SnapStart.
     * This method has no side-effects and can be safely called to trigger SnapStart priming.
     */
    public static void prime() {
        // This method intentionally does nothing but ensures TracingUtils is loaded
        // The actual priming happens in the beforeCheckpoint() method via CRaC hooks
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        // Preload classes first to ensure this always runs
        ClassPreLoader.preloadClasses();
        
        // Initialize key components
        initializeObjectMapper();
        
        // Initialize X-Ray components by accessing them
        AWSXRay.getGlobalRecorder();
        
        // Warm up tracing utilities by calling key methods
        serviceName();
        
        // Initialize ObjectMapper for JSON serialization
        if (objectMapper != null) {
            objectMapper.writeValueAsString("dummy");
        }
    }

    private static synchronized void initializeObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        // No action needed after restore
    }
}
