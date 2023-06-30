package software.amazon.lambda.powertools.idempotency.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.lambda.powertools.idempotency.Idempotent;
import software.amazon.lambda.powertools.idempotency.model.Product;

public class IdempotencyStringFunction implements RequestHandler<Product, String> {

    private boolean handlerCalled = false;

    public boolean handlerCalled() {
        return handlerCalled;
    }

    @Override
    @Idempotent
    public String handleRequest(Product input, Context context) {
        handlerCalled = true;
        return input.getName();
    }
}
