package software.amazon.lambda.powertools.testutils.tracing;

import lombok.NoArgsConstructor;
import software.amazon.lambda.powertools.testutils.tracing.SegmentDocument.SubSegment;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class Trace {
    private final List<SubSegment> subsegments = new ArrayList<>();

    public List<SubSegment> getSubsegments() {
        return subsegments;
    }

    public void addSubSegment(SubSegment subSegment) {
        subsegments.add(subSegment);
    }
}
