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

package software.amazon.lambda.powertools.parameters.secrets;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.FieldSignature;

@Aspect
public class SecretsParamAspect {


    @Pointcut("get(* *) && @annotation(secretsParam)")
    public void getParam(SecretsParam secretsParam) {
    }

    @Around("getParam(secretsParam)")
    public Object injectParam(final ProceedingJoinPoint joinPoint, final SecretsParam secretsParam) {
        System.out.println("GET IT");

        SecretsProvider provider = SecretsProvider.builder()
                .build();

        if (secretsParam.transformer().isInterface()) {
            // No transformation
            return provider.get(secretsParam.key());
        } else {
            FieldSignature s = (FieldSignature) joinPoint.getSignature();
            if (String.class.isAssignableFrom(s.getFieldType())) {
                // Basic transformation
                return provider
                        .withTransformation(secretsParam.transformer())
                        .get(secretsParam.key());
            } else {
                // Complex transformation
                return provider
                        .withTransformation(secretsParam.transformer())
                        .get(secretsParam.key(), s.getFieldType());
            }
        }
    }

}
