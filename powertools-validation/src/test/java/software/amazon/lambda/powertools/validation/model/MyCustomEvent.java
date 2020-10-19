package software.amazon.lambda.powertools.validation.model;

public class MyCustomEvent {
    private Basket basket;

    public MyCustomEvent() {
    }

    public MyCustomEvent(Basket basket) {
        this.basket = basket;
    }

    public Basket getBasket() {
        return basket;
    }

    public void setBasket(Basket basket) {
        this.basket = basket;
    }
}
