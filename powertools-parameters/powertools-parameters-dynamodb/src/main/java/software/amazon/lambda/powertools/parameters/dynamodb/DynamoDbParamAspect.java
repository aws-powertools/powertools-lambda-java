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

import java.util.function.Function;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.FieldSignature;
import software.amazon.lambda.powertools.parameters.BaseParamAspect;
import software.amazon.lambda.powertools.parameters.BaseProvider;

/**
 * Provides the DynamoDB parameter aspect. This aspect is responsible for injecting
 * parameters from DynamoDB into fields annotated with @DynamoDbParam. See the
 * README and Powertools for Lambda (Java) documentation for information on using this feature.
 */
@Aspect
public class DynamoDbParamAspect extends BaseParamAspect {

    private static Function<String, DynamoDbProvider> providerBuilder =
            (String table) -> DynamoDbProvider.builder()
                    .withTable(table)
                    .build();

    @Pointcut("get(* *) && @annotation(ddbConfigParam)")
    public void getParam(DynamoDbParam ddbConfigParam) {
    }

    @Around("getParam(ddbConfigParam)")
    public Object injectParam(final ProceedingJoinPoint joinPoint, final DynamoDbParam ddbConfigParam) {
        System.out.println("GET IT");

        BaseProvider provider = providerBuilder.apply(ddbConfigParam.table());
        return getAndTransform(ddbConfigParam.key(), ddbConfigParam.transformer(), provider, (FieldSignature)joinPoint.getSignature());
    }

}
