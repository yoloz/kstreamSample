package com.ks.error;

/**
 * ks运行中出现的异常,会导致ks退出
 */
public class KRunException extends RuntimeException {

    /**
     * runtime exception
     *
     * @param e e
     */
    public KRunException(Throwable e) {
        super(e);
    }

    public KRunException(String e) {
        super(e);
    }
}
