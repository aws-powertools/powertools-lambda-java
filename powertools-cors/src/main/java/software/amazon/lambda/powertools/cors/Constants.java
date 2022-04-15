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

public interface Constants {
    String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    String VARY = "Vary";
    String VARY_ORIGIN = "Origin";

    String ENV_ACCESS_CONTROL_ALLOW_HEADERS = "ACCESS_CONTROL_ALLOW_HEADERS";
    String ENV_ACCESS_CONTROL_EXPOSE_HEADERS = "ACCESS_CONTROL_EXPOSE_HEADERS";
    String ENV_ACCESS_CONTROL_ALLOW_ORIGIN = "ACCESS_CONTROL_ALLOW_ORIGIN";
    String ENV_ACCESS_CONTROL_ALLOW_METHODS = "ACCESS_CONTROL_ALLOW_METHODS";
    String ENV_ACCESS_CONTROL_ALLOW_CREDENTIALS = "ACCESS_CONTROL_ALLOW_CREDENTIALS";
    String ENV_ACCESS_CONTROL_MAX_AGE = "ACCESS_CONTROL_MAX_AGE";

    String WILDCARD = "*";

    String DEFAULT_ACCESS_CONTROL_ALLOW_HEADERS = "Authorization, *";
    String DEFAULT_ACCESS_CONTROL_EXPOSE_HEADERS = WILDCARD;
    String DEFAULT_ACCESS_CONTROL_ALLOW_METHODS = WILDCARD;
    String DEFAULT_ACCESS_CONTROL_ALLOW_ORIGIN = WILDCARD;
    boolean DEFAULT_ACCESS_CONTROL_ALLOW_CREDENTIALS = true;
    int DEFAULT_ACCESS_CONTROL_MAX_AGE = 29;

}
