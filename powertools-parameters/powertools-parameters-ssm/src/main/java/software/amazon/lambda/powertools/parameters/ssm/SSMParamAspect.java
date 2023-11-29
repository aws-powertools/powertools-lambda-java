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

package software.amazon.lambda.powertools.parameters.ssm;

import java.util.function.Supplier;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.FieldSignature;
import software.amazon.lambda.powertools.parameters.BaseParamAspect;

/**
 * Provides the SSM parameter store parameter aspect. This aspect is responsible for injecting
 * parameters from SSM Parameter Store into fields annotated with @SSMParam. See the
 * README and Powertools for Lambda (Java) documentation for information on using this feature.
 */
@Aspect
public class SSMParamAspect extends BaseParamAspect {

    // This supplier produces a new SSMProvider each time it is called
    private static final Supplier<SSMProvider> providerBuilder = () -> SSMProvider.builder()
            .build();

    @Pointcut("get(* *) && @annotation(secretsParam)")
    public void getParam(SSMParam secretsParam) {
    }

    @Around("getParam(ssmPaam)")
    public Object injectParam(final ProceedingJoinPoint joinPoint, final SSMParam ssmPaam) {
        System.out.println("GET IT");

        SSMProvider provider = providerBuilder.get();
        return getAndTransform(ssmPaam.key(), ssmPaam.transformer(), provider,
                (FieldSignature) joinPoint.getSignature());
    }

}
