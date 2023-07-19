package software.amazon.lambda.powertools.batch.handler;

import com.amazonaws.services.lambda.runtime.Context;

public interface BatchMessageHandler<E, R> {

    public abstract R processBatch(E event, Context context);

}
