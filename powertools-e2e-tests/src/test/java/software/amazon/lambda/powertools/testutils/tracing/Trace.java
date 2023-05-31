package software.amazon.lambda.powertools.testutils.tracing;

import software.amazon.lambda.powertools.testutils.tracing.SegmentDocument.SubSegment;

import java.util.ArrayList;
import java.util.List;

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
