package software.amazon.lambda.powertools.aurora;

import software.amazon.lambda.powertools.aurora.model.PostgresActivityEvent;

@FunctionalInterface
public interface DataStreamHandler <R> {

    R process(PostgresActivityEvent message);
}
