/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.tracing.handlers;

import java.io.IOException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.amazon.lambda.powertools.tracing.Tracing;
import software.amazon.lambda.powertools.tracing.TracingUtils;

import static software.amazon.lambda.powertools.tracing.CaptureMode.RESPONSE;

public class PowerTracerToolEnabledForResponseWithCustomMapper implements RequestHandler<Object, Object> {
    static {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(ChildClass.class, new ChildSerializer());
        objectMapper.registerModule(simpleModule);

        TracingUtils.defaultObjectMapper(objectMapper);
    }
    @Override
    @Tracing(namespace = "lambdaHandler", captureMode = RESPONSE)
    public Object handleRequest(Object input, Context context) {
        ParentClass parentClass = new ParentClass("parent");
        ChildClass childClass = new ChildClass("child", parentClass);
        parentClass.setC(childClass);
        return parentClass;
    }

    public class ParentClass {
        public String name;
        public ChildClass c;

        public ParentClass(String name) {
            this.name = name;
        }

        public void setC(ChildClass c) {
            this.c = c;
        }
    }

    public class ChildClass {
        public String name;
        public ParentClass p;

        public ChildClass(String name, ParentClass p) {
            this.name = name;
            this.p = p;
        }
    }

    public static class ChildSerializer extends StdSerializer<ChildClass> {

        public ChildSerializer() {
            this(null);
        }

        public ChildSerializer(Class<ChildClass> t) {
            super(t);
        }

        @Override
        public void serialize(ChildClass value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("name", value.name);
            jgen.writeStringField("p", value.p.name);
            jgen.writeEndObject();
        }
    }
}
