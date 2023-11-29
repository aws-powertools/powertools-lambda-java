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

package software.amazon.lambda.powertools.parameters.appconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.lambda.powertools.parameters.transform.Transformer;

/**
 * Use this annotation to inject AWS AppConfig parameters into fields in your application. You
 * can also use {@code AppConfigProviderBuilder} to obtain AppConfig values directly, rather than
 * injecting them implicitly.
 * Both {@code environment} and {@code application} fields are necessary.
 *
 * @see AppConfigProviderBuilder
 * @see <a href="https://docs.aws.amazon.com/appconfig>AWS AppConfig</a>
 * @see <a href="https://docs.powertools.aws.dev/lambda/java/utilities/parameters/">Powertools for AWS Lambda (Java) parameters documentation</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AppConfigParam {
    String key();

    /**
     * <b>Mandatory</b>. Provide an environment to the {@link AppConfigProvider}
     */
    String environment();

    /**
     * <b>Mandatory</b>. Provide an application to the {@link AppConfigProvider}
     */
    String application();

    /**
     * <b>Optional</b> Provide a Transformer to transform the returned parameter values.
     */
    Class<? extends Transformer> transformer() default Transformer.class;
}
