package software.amazon.lambda.powertools.validation.model;

import java.util.ArrayList;
import java.util.List;

public class Basket {
    private List<Product> products = new ArrayList<>();

    private String hiddenProduct;

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public Basket() {
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
