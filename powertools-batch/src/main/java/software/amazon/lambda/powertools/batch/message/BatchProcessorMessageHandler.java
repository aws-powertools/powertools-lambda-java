package software.amazon.lambda.powertools.batch.message;

@FunctionalInterface
public interface BatchProcessorMessageHandler<R> {

    R process(BatchProcessorMessage message);
}