/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates.
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

package org.demo.batch.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import org.demo.batch.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.tracing.TracingUtils;

public class AbstractSqsBatchHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSqsBatchHandler.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final String bucket = System.getenv("BUCKET");
    private final S3Client s3 = S3Client.builder().httpClient(UrlConnectionHttpClient.create()).build();
    private final Random r = new Random();

    /**
     * Simulate some processing (I/O + S3 put request)
     * @param p deserialized product
     * @param context Lambda context
     */
    @Logging
    @Tracing
    protected void processMessage(Product p, Context context) {
        TracingUtils.putAnnotation("productId", p.getId());
        TracingUtils.putAnnotation("Thread", Thread.currentThread().getName());
        MDC.put("product", String.valueOf(p.getId()));
        LOGGER.info("Processing product {}", p);

        char c = (char)(r.nextInt(26) + 'a');
        char[] chars = new char[1024 * 1000];
        Arrays.fill(chars, c);
        p.setName(new String(chars));
        try {
            File file = new File("/tmp/"+p.getId()+".json");
            mapper.writeValue(file, p);
            s3.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(p.getId()+".json").build(), RequestBody.fromFile(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            MDC.remove("product");
        }
    }
}
