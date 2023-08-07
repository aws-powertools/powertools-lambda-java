/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package software.amazon.lambda.powertools.testutils.metrics;

import static java.time.Duration.ofSeconds;

import com.evanlennick.retry4j.CallExecutor;
import com.evanlennick.retry4j.CallExecutorBuilder;
import com.evanlennick.retry4j.Status;
import com.evanlennick.retry4j.config.RetryConfig;
import com.evanlennick.retry4j.config.RetryConfigBuilder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * Class in charge of retrieving the actual metrics of a Lambda execution on CloudWatch
 */
public class MetricsFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsFetcher.class);

    private static final SdkHttpClient httpClient = UrlConnectionHttpClient.builder().build();
    private static final Region region = Region.of(System.getProperty("AWS_DEFAULT_REGION", "eu-west-1"));
    private static final CloudWatchClient cloudwatch = CloudWatchClient.builder()
            .httpClient(httpClient)
            .region(region)
            .build();

    /**
     * Retrieve the metric values from start to end. Different parameters are required (see {@link CloudWatchClient#getMetricData} for more info).
     * Use a retry mechanism as metrics may not be available instantaneously after a function runs.
     *
     * @param start
     * @param end
     * @param period
     * @param namespace
     * @param metricName
     * @param dimensions
     * @return
     */
    public List<Double> fetchMetrics(Instant start, Instant end, int period, String namespace, String metricName,
                                     Map<String, String> dimensions) {
        List<Dimension> dimensionsList = new ArrayList<>();
        if (dimensions != null) {
            dimensions.forEach((key, value) -> dimensionsList.add(Dimension.builder().name(key).value(value).build()));
        }

        Callable<List<Double>> callable = () ->
        {
            LOG.debug("Get Metrics for namespace {}, start {}, end {}, metric {}, dimensions {}", namespace, start,
                    end, metricName, dimensionsList);
            GetMetricDataResponse metricData = cloudwatch.getMetricData(GetMetricDataRequest.builder()
                    .startTime(start)
                    .endTime(end)
                    .metricDataQueries(MetricDataQuery.builder()
                            .id(metricName.toLowerCase())
                            .metricStat(MetricStat.builder()
                                    .unit(StandardUnit.COUNT)
                                    .metric(Metric.builder()
                                            .namespace(namespace)
                                            .metricName(metricName)
                                            .dimensions(dimensionsList)
                                            .build())
                                    .period(period)
                                    .stat("Sum")
                                    .build())
                            .returnData(true)
                            .build())
                    .build());
            List<Double> values = metricData.metricDataResults().get(0).values();
            if (values == null || values.isEmpty()) {
                throw new Exception("No data found for metric " + metricName);
            }
            return values;
        };

        RetryConfig retryConfig = new RetryConfigBuilder()
                .withMaxNumberOfTries(10)
                .retryOnAnyException()
                .withDelayBetweenTries(ofSeconds(2))
                .withRandomExponentialBackoff()
                .build();
        CallExecutor<List<Double>> callExecutor = new CallExecutorBuilder<List<Double>>()
                .config(retryConfig)
                .afterFailedTryListener(s ->
                {
                    LOG.warn(s.getLastExceptionThatCausedRetry().getMessage() + ", attempts: " + s.getTotalTries());
                })
                .build();
        Status<List<Double>> status = callExecutor.execute(callable);
        return status.getResult();
    }
}
