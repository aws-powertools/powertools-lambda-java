package software.amazon.lambda.powertools.sqs;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class SQSBatchProcessingException extends RuntimeException {

    private final List<Exception> exceptions;

    public SQSBatchProcessingException(List<Exception> exceptions) {
        this.exceptions = new ArrayList<>(exceptions);
    }

    @Override
    public String getMessage() {
        return exceptions.stream()
                .map(Throwable::getMessage)
                .collect(joining("\n"));
    }
}
