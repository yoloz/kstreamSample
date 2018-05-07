package com.unimas.kstream.webservice.impl.ds;

public enum DSType {

    KAFKA("kafka", 0);


    private String value;
    private int type;

    DSType(String value, int type) {
        this.value = value;
        this.type = type;
    }


    public String getValue() {
        return value;
    }

    public int getType() {
        return type;
    }

    public static int getType(String value) {
        switch (value) {
            case "kafka":
                return 0;
            default:
                throw new IllegalArgumentException("ds_type:[" + value + "] undefined");
        }
    }

    public static String getType(int value) {
        switch (value) {
            case 0:
                return "kafka";
            default:
                throw new IllegalArgumentException("ds_type:[" + value + "] undefined");
        }
    }
}
