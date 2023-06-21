package software.amazon.lambda.powertools.batch;

import software.amazon.lambda.powertools.batch.message.BatchProcessorMessageHandler;
import software.amazon.lambda.powertools.batch.message.BatchProcessorMessageType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BatchMessageProcessor {
    Class<? extends BatchProcessorMessageHandler<Object>> value();

    BatchProcessorMessageType MessageType();

}
