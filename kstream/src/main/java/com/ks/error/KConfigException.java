package com.ks.error;


/**
 * The configuration element error,like null,empty,undefined,etc.
 * this exception will stop kServer
 */
public class KConfigException extends RuntimeException {

    /**
     * Constructs a {@code KConfigException} with the specified
     * detail message.
     *
     * @param s the detail message.
     */
    public KConfigException(String s) {
        super(s);
    }

    public KConfigException(Throwable e) {
        super(e);
    }

}
