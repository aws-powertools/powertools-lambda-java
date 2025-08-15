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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;

public class TestProceedingJoinPoint implements ProceedingJoinPoint {
    private final Signature signature;
    private final Object[] args;

    public TestProceedingJoinPoint(Signature signature, Object[] args) {
        this.signature = signature;
        this.args = args;
    }

    @Override
    public Object[] getArgs() {
        return args;
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    @Override
    public Object getTarget() {
        return null;
    }

    @Override
    public Object getThis() {
        return null;
    }

    @Override
    public StaticPart getStaticPart() {
        return null;
    }

    @Override
    public String getKind() {
        return null;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return null;
    }

    @Override
    public Object proceed() {
        return null;
    }

    @Override
    public Object proceed(Object[] args) {
        return null;
    }

    @Override
    public void set$AroundClosure(AroundClosure arc) {
        // Stubbed method
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
