package software.amazon.lambda.powertools.batch2;


import com.amazonaws.services.lambda.runtime.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface BatchProcessor<EVENT, ITEM, RESPONSE> {

    Logger BATCH_LOGGER = LoggerFactory.getLogger(BatchProcessor.class);

    RESPONSE processBatch(EVENT event, Context context);

    default void processItem(ITEM item, Context context) {
        BATCH_LOGGER.debug("[DEFAULT IMPLEMENTATION] Processing custom item");
    }

}