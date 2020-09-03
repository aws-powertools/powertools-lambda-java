package software.amazon.lambda.powertools.parameters.transform;

public class ObjectToDeserialize {

    public ObjectToDeserialize() {
    }

    private String foo;
    private int bar;
    private long baz;

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

    public int getBar() {
        return bar;
    }

    public void setBar(int bar) {
        this.bar = bar;
    }

    public long getBaz() {
        return baz;
    }

    public void setBaz(long baz) {
        this.baz = baz;
    }
}
