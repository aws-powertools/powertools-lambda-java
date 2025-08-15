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

package software.amazon.lambda.powertools.common.stubs;

import org.aspectj.lang.Signature;

public class TestSignature implements Signature {
    private final Class<?> declaringType;

    public TestSignature(Class<?> declaringType) {
        this.declaringType = declaringType;
    }

    @Override
    public Class<?> getDeclaringType() {
        return declaringType;
    }

    @Override
    public String getDeclaringTypeName() {
        return declaringType.getName();
    }

    @Override
    public int getModifiers() {
        return 0;
    }

    @Override
    public String getName() {
        return "handleRequest";
    }

    @Override
    public String toLongString() {
        return "handleRequest";
    }

    @Override
    public String toShortString() {
        return "handleRequest";
    }
}
