package org.demo.parameters;

public class MyObject {

    private long id;
    private String code;

    public MyObject() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "MyObject{" +
                "id=" + id +
                ", code='" + code + '\'' +
                '}';
    }
}
