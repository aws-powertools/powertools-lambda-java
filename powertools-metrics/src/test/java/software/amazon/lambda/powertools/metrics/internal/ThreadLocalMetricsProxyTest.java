package software.amazon.lambda.powertools.metrics.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.model.DimensionSet;
import software.amazon.lambda.powertools.metrics.model.MetricResolution;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;
import software.amazon.lambda.powertools.metrics.provider.MetricsProvider;

/**
 * Tests for ThreadLocalMetricsProxy focusing on lazy vs eager initialization behavior.
 * 
 * CRITICAL: These tests ensure configuration methods (setNamespace, setDefaultDimensions, 
 * setRaiseOnEmptyMetrics) do NOT eagerly create instances, while metrics operations 
 * (addMetric, addDimension, flush) DO eagerly create instances.
 */
@ExtendWith(MockitoExtension.class)
class ThreadLocalMetricsProxyTest {

    @Mock(strictness = Strictness.LENIENT)
    private MetricsProvider mockProvider;

    @Mock(strictness = Strictness.LENIENT)
    private Metrics mockMetrics;

    private ThreadLocalMetricsProxy proxy;

    @BeforeEach
    void setUp() {
        when(mockProvider.getMetricsInstance()).thenReturn(mockMetrics);
        proxy = new ThreadLocalMetricsProxy(mockProvider);
    }

    // ========== LAZY INITIALIZATION TESTS (Configuration Methods) ==========

    @Test
    void setNamespace_shouldNotEagerlyCreateInstance() {
        // WHEN
        proxy.setNamespace("TestNamespace");

        // THEN - Provider should NOT be called (lazy initialization)
        verify(mockProvider, never()).getMetricsInstance();
    }

    @Test
    void setDefaultDimensions_shouldNotEagerlyCreateInstance() {
        // WHEN
        proxy.setDefaultDimensions(DimensionSet.of("key", "value"));

        // THEN - Provider should NOT be called (lazy initialization)
        verify(mockProvider, never()).getMetricsInstance();
    }

    @Test
    void setDefaultDimensions_shouldThrowExceptionWhenNull() {
        // When/Then
        assertThatThrownBy(() -> proxy.setDefaultDimensions(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DimensionSet cannot be null");
    }

    @Test
    void setRaiseOnEmptyMetrics_shouldNotEagerlyCreateInstance() {
        // WHEN
        proxy.setRaiseOnEmptyMetrics(true);

        // THEN - Provider should NOT be called (lazy initialization)
        verify(mockProvider, never()).getMetricsInstance();
    }

    // ========== EAGER INITIALIZATION TESTS (Metrics Operations) ==========

    @Test
    void addMetric_shouldEagerlyCreateInstance() {
        // WHEN
        proxy.addMetric("test", 1, MetricUnit.COUNT, MetricResolution.HIGH);

        // THEN - Provider SHOULD be called (eager initialization)
        verify(mockProvider, times(1)).getMetricsInstance();
        verify(mockMetrics).addMetric("test", 1, MetricUnit.COUNT, MetricResolution.HIGH);
    }

    @Test
    void addDimension_shouldEagerlyCreateInstance() {
        // WHEN
        proxy.addDimension(DimensionSet.of("key", "value"));

        // THEN - Provider SHOULD be called (eager initialization)
        verify(mockProvider, times(1)).getMetricsInstance();
        verify(mockMetrics).addDimension(any(DimensionSet.class));
    }

    @Test
    void addMetadata_shouldEagerlyCreateInstance() {
        // WHEN
        proxy.addMetadata("key", "value");

        // THEN - Provider SHOULD be called (eager initialization)
        verify(mockProvider, times(1)).getMetricsInstance();
        verify(mockMetrics).addMetadata("key", "value");
    }

    @Test
    void flush_shouldAlwaysCreateInstance() {
        // WHEN
        proxy.flush();

        // THEN - Provider SHOULD be called even if no metrics added
        verify(mockProvider, times(1)).getMetricsInstance();
        verify(mockMetrics).flush();
    }

    // ========== CONFIGURATION APPLIED ON FIRST METRICS OPERATION ==========

    @Test
    void firstMetricsOperation_shouldApplyStoredConfiguration() {
        // GIVEN - Set configuration without creating instance
        proxy.setNamespace("TestNamespace");
        proxy.setDefaultDimensions(DimensionSet.of("Service", "TestService"));
        proxy.setRaiseOnEmptyMetrics(true);
        verify(mockProvider, never()).getMetricsInstance();

        // WHEN - First metrics operation
        proxy.addMetric("test", 1, MetricUnit.COUNT);

        // THEN - Instance created and configuration applied
        verify(mockProvider, times(1)).getMetricsInstance();
        verify(mockMetrics).setNamespace("TestNamespace");
        verify(mockMetrics).setDefaultDimensions(any(DimensionSet.class));
        verify(mockMetrics).setRaiseOnEmptyMetrics(true);
        verify(mockMetrics).addMetric("test", 1, MetricUnit.COUNT, MetricResolution.STANDARD);
    }

    // ========== THREAD ISOLATION TESTS ==========

    @Test
    void differentThreads_shouldInheritParentInstance() throws Exception {
        // WHEN - Parent thread adds metric
        proxy.addMetric("metric1", 1, MetricUnit.COUNT);
        verify(mockProvider, times(1)).getMetricsInstance();

        // WHEN - Child thread adds metric (inherits parent's instance)
        Thread thread2 = new Thread(() -> {
            proxy.addMetric("metric2", 2, MetricUnit.COUNT);
        });
        thread2.start();
        thread2.join();

        // THEN - Only one instance created (child inherits via InheritableThreadLocal)
        verify(mockProvider, times(1)).getMetricsInstance();
    }

    @Test
    void flush_shouldRemoveThreadLocalInstance() {
        // GIVEN - Create instance
        proxy.addMetric("test", 1, MetricUnit.COUNT);
        verify(mockProvider, times(1)).getMetricsInstance();

        // WHEN - Flush
        proxy.flush();

        // WHEN - Add another metric (should create new instance)
        proxy.addMetric("test2", 2, MetricUnit.COUNT);

        // THEN - Provider called twice (instance was removed after flush)
        verify(mockProvider, times(2)).getMetricsInstance();
    }

    // ========== EDGE CASES ==========

    @Test
    void multipleConfigurationCalls_shouldUpdateAtomicReferences() {
        // WHEN
        proxy.setNamespace("Namespace1");
        proxy.setNamespace("Namespace2");
        proxy.setNamespace("Namespace3");

        // THEN - No instance created
        verify(mockProvider, never()).getMetricsInstance();

        // WHEN - First metrics operation
        proxy.addMetric("test", 1, MetricUnit.COUNT);

        // THEN - Only last namespace applied
        verify(mockMetrics).setNamespace("Namespace3");
        verify(mockMetrics, never()).setNamespace("Namespace1");
        verify(mockMetrics, never()).setNamespace("Namespace2");
    }

    @Test
    void configurationAfterInstanceCreation_shouldApplyImmediately() {
        // GIVEN - Instance already created
        proxy.addMetric("test", 1, MetricUnit.COUNT);

        // WHEN - Set configuration
        proxy.setNamespace("NewNamespace");

        // THEN - Applied immediately to existing instance
        verify(mockMetrics).setNamespace("NewNamespace");
    }

    @Test
    void setTimestamp_shouldEagerlyCreateInstance() {
        // When
        proxy.setTimestamp(Instant.now());

        // Then
        verify(mockProvider, times(1)).getMetricsInstance();
        verify(mockMetrics).setTimestamp(any());
    }

    @Test
    void getDefaultDimensions_shouldNotEagerlyCreateInstance() {
        // WHEN
        DimensionSet result = proxy.getDefaultDimensions();

        // THEN - Provider should NOT be called (returns stored config or empty)
        verify(mockProvider, never()).getMetricsInstance();
        assertThat(result).isNotNull();
    }

    @Test
    void captureColdStartMetric_shouldEagerlyCreateInstance() {
        // WHEN
        proxy.captureColdStartMetric(DimensionSet.of("key", "value"));

        // THEN - Provider SHOULD be called (eager initialization)
        verify(mockProvider, times(1)).getMetricsInstance();
        verify(mockMetrics).captureColdStartMetric(any(DimensionSet.class));
    }

    @Test
    void flushMetrics_shouldEagerlyCreateInstance() {
        // WHEN
        proxy.flushMetrics(m -> m.addMetric("test", 1, MetricUnit.COUNT));

        // THEN - Provider SHOULD be called (eager initialization)
        verify(mockProvider, times(1)).getMetricsInstance();
        verify(mockMetrics).flushMetrics(any());
    }

    @Test
    void clearDefaultDimensions_shouldEagerlyCreateInstance() {
        // WHEN
        proxy.clearDefaultDimensions();

        // THEN - Provider SHOULD be called (eager initialization)
        verify(mockProvider, times(1)).getMetricsInstance();
        verify(mockMetrics).clearDefaultDimensions();
    }
}
