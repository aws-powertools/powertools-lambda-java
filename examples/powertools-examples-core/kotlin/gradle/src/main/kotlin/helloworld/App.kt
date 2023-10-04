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
package helloworld

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.xray.entities.Subsegment
import org.apache.logging.log4j.LogManager
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger
import software.amazon.cloudwatchlogs.emf.model.DimensionSet
import software.amazon.cloudwatchlogs.emf.model.Unit
import software.amazon.lambda.powertools.logging.Logging
import software.amazon.lambda.powertools.logging.LoggingUtils
import software.amazon.lambda.powertools.metrics.Metrics
import software.amazon.lambda.powertools.metrics.MetricsUtils
import software.amazon.lambda.powertools.tracing.CaptureMode
import software.amazon.lambda.powertools.tracing.Tracing
import software.amazon.lambda.powertools.tracing.TracingUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.stream.Collectors

/**
 * Handler for requests to Lambda function.
 */

class App : RequestHandler<APIGatewayProxyRequestEvent?, APIGatewayProxyResponseEvent> {
    @Logging(logEvent = true, samplingRate = 0.7)
    @Tracing(captureMode = CaptureMode.RESPONSE_AND_ERROR)
    @Metrics(namespace = "ServerlessAirline", service = "payment", captureColdStart = true)

    override fun handleRequest(input: APIGatewayProxyRequestEvent?, context: Context?): APIGatewayProxyResponseEvent { // changed context to be optional so can pass null
        val headers = mutableMapOf<String, String>()
        headers["Content-Type"] = "application/json"
        headers["X-Custom-Header"] = "application/json"
        MetricsUtils.metricsLogger().putMetric("CustomMetric1", 1.0, Unit.COUNT)
        MetricsUtils.withSingleMetric("CustomMetrics2", 1.0, Unit.COUNT, "Another") { metric: MetricsLogger ->
            metric.setDimensions(DimensionSet.of("AnotherService", "CustomService"))
            metric.setDimensions(DimensionSet.of("AnotherService1", "CustomService1"))
        }
        LoggingUtils.appendKey("test", "willBeLogged")
        val response = APIGatewayProxyResponseEvent().withHeaders(headers)
        return try {
            val pageContents = getPageContents("https://checkip.amazonaws.com")
            log.info(pageContents)
            TracingUtils.putAnnotation("Test", "New")
            val output = """
            {
                "message": "hello world", 
                "location": "$pageContents"
            }
            """.trimIndent()
            TracingUtils.withSubsegment("loggingResponse") { _: Subsegment? ->
                val sampled = "log something out"
                log.info(sampled)
                log.info(output)
            }
            log.info("After output")
            response.withStatusCode(200).withBody(output)
        } catch (e: RuntimeException) {
            response.withBody("{}").withStatusCode(500)
        } catch (e: IOException) {
            response.withBody("{}").withStatusCode(500)
        }
    }

    @Tracing
    private fun log() {
        log.info("inside threaded logging for function")
    }

    @Tracing(namespace = "getPageContents", captureMode = CaptureMode.DISABLED)
    @Throws(IOException::class)
    private fun getPageContents(address: String): String {
        val url = URL(address)
        TracingUtils.putMetadata("getPageContents", address)
        BufferedReader(InputStreamReader(url.openStream())).use { br -> return br.lines().collect(Collectors.joining(System.lineSeparator())) }
    }

    private val log = LogManager.getLogger(App::class)
}
