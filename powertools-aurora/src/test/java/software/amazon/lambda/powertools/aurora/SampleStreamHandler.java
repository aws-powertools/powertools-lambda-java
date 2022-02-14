package software.amazon.lambda.powertools.aurora;

import software.amazon.lambda.powertools.aurora.model.PostgresActivityEvent;

public class SampleStreamHandler implements DataStreamHandler<Object>{

    @Override
    public Object process(PostgresActivityEvent message) {
        return new PostgresActivityEvent();
    }
}
