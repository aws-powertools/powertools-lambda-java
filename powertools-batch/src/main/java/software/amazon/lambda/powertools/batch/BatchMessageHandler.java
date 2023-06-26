import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.batch.BatchMessageHandlerBuilder;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

/**
 *
 * @param <T>
 */
public abstract class BatchMessageHandler<T, U> implements RequestHandler<T, List<String>> {


    public BatchMessageHandler() {
    }

    @Override
    public List<String> handleRequest(T t, Context context) {
        throw new NotImplementedException();
    }

    public abstract U processMessage(T message);
}
