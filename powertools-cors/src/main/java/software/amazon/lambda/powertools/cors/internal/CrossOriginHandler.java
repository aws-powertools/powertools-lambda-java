/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.cors.internal;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.cors.CrossOrigin;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static software.amazon.lambda.powertools.cors.Constants.*;
import static software.amazon.lambda.powertools.cors.Constants.ACCESS_CONTROL_MAX_AGE;

public class CrossOriginHandler {
    private static final Logger LOG = LogManager.getLogger(CrossOriginHandler.class);

    private final String allowHeaders;
    private final String exposedHeaders;
    private final String allowMethods;
    private final String allowOrigins;
    private final String allowCredentials;
    private final String maxAge;

    CrossOriginHandler(CrossOrigin crossOrigin) {
         allowHeaders = Optional.ofNullable(System.getenv(ENV_ACCESS_CONTROL_ALLOW_HEADERS)).orElse(crossOrigin.allowedHeaders());
         exposedHeaders = Optional.ofNullable(System.getenv(ENV_ACCESS_CONTROL_EXPOSE_HEADERS)).orElse(crossOrigin.exposedHeaders());
         allowMethods = Optional.ofNullable(System.getenv(ENV_ACCESS_CONTROL_ALLOW_METHODS)).orElse(crossOrigin.methods());
         allowOrigins = Optional.ofNullable(System.getenv(ENV_ACCESS_CONTROL_ALLOW_ORIGIN)).orElse(crossOrigin.origins());
         allowCredentials = Optional.ofNullable(System.getenv(ENV_ACCESS_CONTROL_ALLOW_CREDENTIALS)).orElse(String.valueOf(crossOrigin.allowCredentials()));
         maxAge = Optional.ofNullable(System.getenv(ENV_ACCESS_CONTROL_MAX_AGE)).orElse(String.valueOf(crossOrigin.maxAge()));
    }

    public APIGatewayProxyResponseEvent process(APIGatewayProxyRequestEvent request, APIGatewayProxyResponseEvent response) {
        try {
            String origin = request.getHeaders().get("origin");
            LOG.debug("origin=" + origin);

            if (response.getHeaders() == null) {
                response.setHeaders(new HashMap<>());
            }
            processOrigin(response, origin);
            response.getHeaders().put(ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
            response.getHeaders().put(ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders);
            response.getHeaders().put(ACCESS_CONTROL_ALLOW_METHODS, allowMethods);
            response.getHeaders().put(ACCESS_CONTROL_ALLOW_CREDENTIALS, allowCredentials);
            response.getHeaders().put(ACCESS_CONTROL_MAX_AGE, maxAge);
        } catch (Exception e) {
            // should not happen, but we don't want to fail because of this
            LOG.error("Error while setting CORS headers. If you think this is an issue in PowerTools, please open an issue on GitHub", e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("====== CORS Headers ======");
            response.getHeaders().forEach((key, value) -> {if (key.startsWith("Access-Control")) { LOG.debug(key + " -> " + value); }} );
        }
        return response;
    }

    private void processOrigin(APIGatewayProxyResponseEvent response, String origin) throws MalformedURLException {
        if (allowOrigins != null) {
            List<String> allowOriginList = Arrays.asList(allowOrigins.split("\\s*,\\s*"));
            if (allowOriginList.stream().anyMatch(WILDCARD::equals)) {
                response.getHeaders().put(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                response.getHeaders().put(VARY, VARY_ORIGIN);
            } else {
                URL url = new URL(origin);
                allowOriginList.stream().filter(o -> {
                    try {
                        URL allowUrl = new URL(o);
                        return url.getProtocol().equals(allowUrl.getProtocol()) &&
                                url.getPort() == allowUrl.getPort() &&
                                (url.getHost().equals(allowUrl.getHost()) || allowUrl.getHost().equals(WILDCARD));
                    } catch (MalformedURLException e) {
                        LOG.warn("Allowed origin '"+o+"' is malformed. It should contain a protocol, a host and eventually a port");
                        return false;
                    }
                }).findAny().ifPresent(validOrigin -> {
                    response.getHeaders().put(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                    response.getHeaders().put(VARY, VARY_ORIGIN);
                });
            }
        }
    }
}
