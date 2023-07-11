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
package software.amazon.lambda.powertools.utilities.model;

import java.util.HashMap;
import java.util.Map;

public class Order {
    private Map<String, Product> orders = new HashMap<>();

    public Order() {
    }

    public Map<String, Product> getProducts() {
        return orders;
    }

    public void setProducts(Map<String, Product> products) {
        this.orders = products;
    }

}
