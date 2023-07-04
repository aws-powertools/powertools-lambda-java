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
package software.amazon.lambda.powertools.validation.model;

import java.util.ArrayList;
import java.util.List;

public class Basket {
    private List<Product> products = new ArrayList<>();

    private String hiddenProduct;

    public Basket() {
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public void add(Product product) {
        products.add(product);
    }

    public String getHiddenProduct() {
        return hiddenProduct;
    }

    public void setHiddenProduct(String hiddenProduct) {
        this.hiddenProduct = hiddenProduct;
    }
}
