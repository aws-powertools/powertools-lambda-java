package software.amazon.lambda.powertools.testutils.tracing;

import java.util.ArrayList;
import java.util.List;
import software.amazon.lambda.powertools.testutils.tracing.SegmentDocument.SubSegment;

public class Trace {
    private final List<SubSegment> subsegments = new ArrayList<>();

    public Trace() {
    }

    public List<SubSegment> getSubsegments() {
        return subsegments;
    }

    public void addSubSegment(SubSegment subSegment) {
        subsegments.add(subSegment);
    }
}
