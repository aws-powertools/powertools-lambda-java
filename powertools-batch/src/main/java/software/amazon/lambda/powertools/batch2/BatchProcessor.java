package software.amazon.lambda.powertools.batch2;


import com.amazonaws.services.lambda.runtime.Context;

public interface BatchProcessor<EVENT, ITEM, RESPONSE> {

    RESPONSE processBatch(EVENT event, Context context);

    void processItem(ITEM item, Context context);

}