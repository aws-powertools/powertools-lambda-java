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

package software.amazon.lambda.powertools.parameters.dynamodb;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

/**
 * Inject a parameter from the DynamoDB Parameter Store into a field. You can also use
 * {@code DynamoDbProviderBuilder} to obtain DynamoDB values directly, rather than injecting them implicitly.
 *
 * Usage:
 * <pre>
 * @SSMParam(key = "my-param", table = "my-table")
 * String myParameter;
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DynamoDbParam {
    /**
     * <b>Mandatory</b>. Partition key from the DynamoDB table
     */
    String key();

    /**
     * <b>Mandatory</b>. Table name for the DynamoDB table
     * @return
     */
    String table();

    /**
     * <b>Optional</b> Provide a Transformer to transform the returned parameter values.
     */
    Class<? extends Transformer> transformer() default Transformer.class;
}
