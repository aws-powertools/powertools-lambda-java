package software.amazon.lambda.powertools.sqs;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;

import static java.util.stream.Collectors.joining;

public class SQSBatchProcessingException extends RuntimeException {

    private final List<Exception> exceptions;
    private final List<SQSEvent.SQSMessage> failures;
    private final List<Object> returnValues;

    public <T> SQSBatchProcessingException(final List<Exception> exceptions,
                                           final List<SQSEvent.SQSMessage> failures,
                                           final List<T> successReturns) {
        super(exceptions.stream()
                .map(Throwable::toString)
                .collect(joining("\n")));

        this.exceptions = new ArrayList<>(exceptions);
        this.failures = new ArrayList<>(failures);
        this.returnValues = new ArrayList<>(successReturns);
    }

    public List<Exception> getExceptions() {
        return exceptions;
    }

    public List<Object> successMessageReturnValues() {
        return returnValues;
    }

    public List<SQSEvent.SQSMessage> getFailures() {
        return failures;
    }

    @Override
    public void printStackTrace() {
        for (Exception exception : exceptions) {
            exception.printStackTrace();
        }
    }

    <T> void addSuccessMessageReturnValues(final List<T> returnValues) {
        this.returnValues.addAll(returnValues);
    }
}
