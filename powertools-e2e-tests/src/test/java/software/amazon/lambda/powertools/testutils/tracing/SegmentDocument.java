package software.amazon.lambda.powertools.testutils.tracing;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Setter
@Getter
public class SegmentDocument {
    private String id;

    @JsonSetter("trace_id")
    private String traceId;

    private String name;

    @JsonSetter("start_time")
    private long startTime;

    @JsonSetter("end_time")
    private long endTime;

    private String origin;

    private Aws aws;

    private List<SubSegment> subsegments = new ArrayList<>();

    public Duration getDuration() {
        return Duration.ofMillis(endTime - startTime);
    }

    public boolean hasSubsegments() {
        return !subsegments.isEmpty();
    }

    @NoArgsConstructor
    @Setter
    @Getter
    public static class Aws{
        @JsonSetter("account_id")
        private long accountId;

        @JsonSetter("function_arn")
        private String functionArn;

        @JsonSetter("resource_names")
        private String[] resourceNames;
    }

    @NoArgsConstructor
    @Setter
    @Getter
    public static class SubSegment{
        private String id;

        private String name;

        @JsonSetter("start_time")
        private long startTime;

        @JsonSetter("end_time")
        private long endTime;

        private List<SubSegment> subsegments = new ArrayList<>();

        private Map<String, Object> annotations;

        private Map<String, Object> metadata;

        private String namespace;

        public boolean hasSubsegments() {
            return !subsegments.isEmpty();
        }

        public Duration getDuration() {
            return Duration.ofMillis(endTime - startTime);
        }
    }
}
