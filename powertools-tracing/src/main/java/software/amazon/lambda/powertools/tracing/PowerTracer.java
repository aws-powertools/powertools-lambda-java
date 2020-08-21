package software.amazon.lambda.powertools.tracing;

import java.util.function.Consumer;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Subsegment;

import static software.amazon.lambda.powertools.core.internal.LambdaHandlerProcessor.serviceName;

public final class PowerTracer {

    public static void putAnnotation(String key, String value) {
        AWSXRay.getCurrentSubsegmentOptional()
                .ifPresent(segment -> segment.putAnnotation(key, value));
    }

    public static void putMetadata(String key, Object value) {
        String namespace = AWSXRay.getCurrentSubsegmentOptional()
                .map(Subsegment::getNamespace).orElse(serviceName());

        putMetadata(namespace, key, value);
    }

    public static void putMetadata(String namespace, String key, Object value) {
        AWSXRay.getCurrentSubsegmentOptional()
                .ifPresent(segment -> segment.putMetadata(namespace, key, value));
    }

    public static void withEntitySubsegment(String name, Entity entity, Consumer<Subsegment> subsegment) {
        AWSXRay.setTraceEntity(entity);
        withEntitySubsegment(serviceName(), name, entity, subsegment);
    }

    public static void withEntitySubsegment(String namespace, String name, Entity entity, Consumer<Subsegment> subsegment) {
        AWSXRay.setTraceEntity(entity);
        withSubsegment(namespace, name, subsegment);
    }

    public static void withSubsegment(String name, Consumer<Subsegment> subsegment) {
        withSubsegment(serviceName(), name, subsegment);
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
