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

package software.amazon.lambda.powertools.common.internal;

public class LambdaConstants {
    public static final String LAMBDA_FUNCTION_NAME_ENV = "AWS_LAMBDA_FUNCTION_NAME";
    public static final String AWS_REGION_ENV = "AWS_REGION";
    public static final String X_AMZN_TRACE_ID = "_X_AMZN_TRACE_ID";
    public static final String XRAY_TRACE_HEADER = "com.amazonaws.xray.traceHeader";
    public static final String AWS_SAM_LOCAL = "AWS_SAM_LOCAL";
    public static final String ROOT_EQUALS = "Root=";
    public static final String POWERTOOLS_SERVICE_NAME = "POWERTOOLS_SERVICE_NAME";
    public static final String SERVICE_UNDEFINED = "service_undefined";
}
