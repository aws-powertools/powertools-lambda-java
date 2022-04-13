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
package software.amazon.lambda.powertools.cors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static software.amazon.lambda.powertools.cors.Constants.*;

/**
 * CrossOrigin annotation to be placed on a Lambda function configured with API Gateway as proxy.<br/>
 * Your function must implement <pre>RequestHandler&lt;APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent&gt;</pre>
 * It will automatically add the Cross-Origins Resource Sharing (CORS) headers in the response sent to API Gateway & the client.<br/>
 * By default, it allows everything (all methods, headers and origins). Make sure to restrict to your need.
 * <p></p>You can use the annotation alone and keep the default setup (or use environment variables instead, see below):<br/>
 * <pre>
 *    &#64;CrossOrigin
 *    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context ctx) {
 *      // ...
 *      return response;
 *    }
 * </pre>
 * </p>
 * <p>You can use the annotation and customize the parameters:<br/>
 * <pre>
 *    &#64;CrossOrigin(
 *         origins = "origin.com",
 *         allowedHeaders = "Content-Type",
 *         methods = "POST, OPTIONS"
 *     )
 *    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context ctx) {
 *      // ...
 *      return response;
 *    }
 * </pre>
 * </p>
 * <p>You can also use the following environment variables if you wish to externalize the configuration:<ul>
 *     <li><pre>ACCESS_CONTROL_ALLOW_HEADERS</pre></li>
 *     <li><pre>ACCESS_CONTROL_EXPOSE_HEADERS</pre></li>
 *     <li><pre>ACCESS_CONTROL_ALLOW_ORIGIN</pre></li>
 *     <li><pre>ACCESS_CONTROL_ALLOW_METHODS</pre></li>
 *     <li><pre>ACCESS_CONTROL_ALLOW_CREDENTIALS</pre></li>
 *     <li><pre>ACCESS_CONTROL_MAX_AGE</pre></li>
 * </ul></p>
 * Note that if you configure cross-origin both in environment variables and programmatically with the annotation,
 * environment variables take the precedence over the annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CrossOrigin {
    /**
     * Allowed methods (Access-Control-Request-Methods). You can set several methods separated by a comma (',').
     * Default: * (allow all methods)
     */
    String methods() default DEFAULT_ACCESS_CONTROL_ALLOW_METHODS;
    /**
     * Allowed origins (Access-Control-Allow-Origin). You can set several origins separated by a comma (',').
     * If you do so, only the origin matching the client origin will be sent in the response.
     * An origin must be a well-formed url: {protocol}://{host}[:{port}]
     * Default: * (allow all origins)
     */
    String origins() default DEFAULT_ACCESS_CONTROL_ALLOW_ORIGIN;
    /**
     * Allowed headers (Access-Control-Request-Headers). You can set several headers separated by a comma (',').
     * Note that Authorization is not part of the wildcard and must be set explicitly
     * Default: Authorization, * (allow all headers)
     */
    String allowedHeaders() default DEFAULT_ACCESS_CONTROL_ALLOW_HEADERS;
    /**
     * Exposed headers (Access-Control-Expose-Headers). You can set several headers separated by a comma (',').
     * Default: * (expose all headers)
     */
    String exposedHeaders() default DEFAULT_ACCESS_CONTROL_EXPOSE_HEADERS;
    /**
     * If credential mode is allowed (ACCESS_CONTROL_ALLOW_CREDENTIALS)
     * Default: true
     */
    boolean allowCredentials() default DEFAULT_ACCESS_CONTROL_ALLOW_CREDENTIALS;
    /**
     * How long (in seconds) the preflight request is cached (default: 29)
     */
    int maxAge() default DEFAULT_ACCESS_CONTROL_MAX_AGE;
}
