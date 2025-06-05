package software.amazon.lambda.powertools.metrics.testutils;

import java.util.Collections;

import com.amazonaws.services.lambda.runtime.Context;

import software.amazon.lambda.powertools.metrics.MetricsLogger;
import software.amazon.lambda.powertools.metrics.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.model.MetricResolution;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

public class TestMetricsLogger implements MetricsLogger {
    @Override
    public void addMetric(String name, double value, MetricUnit unit) {
        // Test placeholder
    }

    @Override
    public void flush() {
        // Test placeholder
    }

    @Override
    public void addMetric(String key, double value, MetricUnit unit, MetricResolution resolution) {
        // Test placeholder
    }

    @Override
    public void addDimension(DimensionSet dimensionSet) {
        // Test placeholder
    }

    @Override
    public void addMetadata(String key, Object value) {
        // Test placeholder
    }

    @Override
    public void setDefaultDimensions(DimensionSet dimensionSet) {
        // Test placeholder
    }

    @Override
    public DimensionSet getDefaultDimensions() {
        // Test placeholder
        return DimensionSet.of(Collections.emptyMap());
    }

    @Override
    public void setNamespace(String namespace) {
        // Test placeholder
    }

    @Override
    public void setRaiseOnEmptyMetrics(boolean raiseOnEmptyMetrics) {
        // Test placeholder
    }

    @Override
    public void clearDefaultDimensions() {
        // Test placeholder
    }

    @Override
    public void captureColdStartMetric(Context context, DimensionSet dimensions) {
        // Test placeholder
    }

    @Override
    public void captureColdStartMetric(DimensionSet dimensions) {
        // Test placeholder
    }

    @Override
    public void flushSingleMetric(String name, double value, MetricUnit unit, String namespace,
            DimensionSet dimensions) {
        // Test placeholder
    }
}
