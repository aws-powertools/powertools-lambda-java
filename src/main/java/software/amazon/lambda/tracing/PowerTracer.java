package software.amazon.lambda.tracing;

import java.util.function.Consumer;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Subsegment;

public final class PowerTracer {
    public static final String SERVICE_NAME = null != System.getenv("POWERTOOLS_SERVICE_NAME")
            ? System.getenv("POWERTOOLS_SERVICE_NAME") : "service_undefined";

    public static void putAnnotation(String key, String value) {
        AWSXRay.getCurrentSubsegmentOptional()
                .ifPresent(segment -> segment.putAnnotation(key, value));
    }

    public static void putMetadata(String key, Object value) {
        String namespace = AWSXRay.getCurrentSubsegmentOptional()
                .map(Subsegment::getNamespace).orElse(SERVICE_NAME);

        putMetadata(namespace, key, value);
    }

    public static void putMetadata(String namespace, String key, Object value) {
        AWSXRay.getCurrentSubsegmentOptional()
                .ifPresent(segment -> segment.putMetadata(namespace, key, value));
    }

    public static void withEntitySubsegment(String name, Entity entity, Consumer<Subsegment> subsegment) {
        AWSXRay.setTraceEntity(entity);
        withEntitySubsegment(SERVICE_NAME, name, entity, subsegment);
    }

    public static void withEntitySubsegment(String namespace, String name, Entity entity, Consumer<Subsegment> subsegment) {
        AWSXRay.setTraceEntity(entity);
        withSubsegment(namespace, name, subsegment);
    }

    public static void withSubsegment(String name, Consumer<Subsegment> subsegment) {
        withSubsegment(SERVICE_NAME, name, subsegment);
    }

    public static void withSubsegment(String namespace, String name, Consumer<Subsegment> subsegment) {
        Subsegment segment = AWSXRay.beginSubsegment("## " + name);
        segment.setNamespace(namespace);
        try {
            subsegment.accept(segment);
        } finally {
            AWSXRay.endSubsegment();
        }
    }
}
