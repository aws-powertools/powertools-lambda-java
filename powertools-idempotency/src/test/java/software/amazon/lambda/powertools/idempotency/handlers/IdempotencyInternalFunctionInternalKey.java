/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.
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
import software.amazon.lambda.powertools.idempotency.Idempotent;
import software.amazon.lambda.powertools.idempotency.model.Basket;
import software.amazon.lambda.powertools.idempotency.model.Product;

/**
 * Simple Lambda function with @{@link Idempotent} annotation on a sub method (not the handleRequest
 * one)
 */
public class IdempotencyInternalFunctionInternalKey implements RequestHandler<Product, Basket> {

    @Override
    public Basket handleRequest(Product input, Context context) {
        return createBasket(input);
    }

    @Idempotent
    private Basket createBasket(Product p) {
        return new Basket(p);
    }
}
