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

package software.amazon.lambda.powertools.idempotency.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import software.amazon.lambda.powertools.idempotency.Idempotency;
import software.amazon.lambda.powertools.idempotency.model.Basket;
import software.amazon.lambda.powertools.idempotency.model.Product;

/**
 * Lambda function using Idempotency functional API with explicit idempotency key
 */
public class IdempotencyMultiArgFunctionalFunction implements RequestHandler<Product, Basket> {

    private boolean processCalled = false;
    private String extraData;

    public boolean processCalled() {
        return processCalled;
    }

    public String getExtraData() {
        return extraData;
    }

    @Override
    public Basket handleRequest(Product input, Context context) {
        Idempotency.registerLambdaContext(context);

        return Idempotency.makeIdempotent(input.getId(), () -> process(input, "extra-data"), Basket.class);
    }

    private Basket process(Product input, String extraData) {
        processCalled = true;
        this.extraData = extraData;
        Basket b = new Basket();
        b.add(input);
        return b;
    }
}
