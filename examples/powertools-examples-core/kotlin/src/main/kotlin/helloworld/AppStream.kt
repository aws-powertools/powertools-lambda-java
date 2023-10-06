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
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.lambda.powertools.logging.Logging
import software.amazon.lambda.powertools.metrics.Metrics
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class AppStream : RequestStreamHandler {
    @Logging(logEvent = true)
    @Metrics(namespace = "ServerlessAirline", service = "payment", captureColdStart = true)
    @Throws(IOException::class)
    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        val map: Map<*, *> = mapper.readValue(input, MutableMap::class.java)
        println(map.size)
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}
