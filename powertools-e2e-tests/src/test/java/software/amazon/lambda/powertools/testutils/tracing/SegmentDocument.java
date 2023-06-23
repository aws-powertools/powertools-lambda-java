package software.amazon.lambda.powertools.testutils.tracing;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private List<SubSegment> subsegments = new ArrayList<>();

    public SegmentDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public List<SubSegment> getSubsegments() {
        return subsegments;
    }

    public void setSubsegments(List<SubSegment> subsegments) {
        this.subsegments = subsegments;
    }

    public Duration getDuration() {
        return Duration.ofMillis(endTime - startTime);
    }

    public boolean hasSubsegments() {
        return !subsegments.isEmpty();
    }

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

        public SubSegment() {
        }

        public boolean hasSubsegments() {
            return !subsegments.isEmpty();
        }

        public Duration getDuration() {
            return Duration.ofMillis(endTime - startTime);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public List<SubSegment> getSubsegments() {
            return subsegments;
        }

        public void setSubsegments(List<SubSegment> subsegments) {
            this.subsegments = subsegments;
        }

        public Map<String, Object> getAnnotations() {
            return annotations;
        }

        public void setAnnotations(Map<String, Object> annotations) {
            this.annotations = annotations;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
    }
}
