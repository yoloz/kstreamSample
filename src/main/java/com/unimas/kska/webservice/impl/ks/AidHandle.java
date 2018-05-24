package com.unimas.kska.webservice.impl.ks;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class AidHandle {

    private final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final MethodType type = MethodType.methodType(void.class, HttpServletRequest.class, HttpServletResponse.class);

    public void handle(Class<?> clazz, String method, HttpServletRequest request, HttpServletResponse response)
            throws Throwable {
        MethodHandle mh = lookup.findVirtual(clazz, method, type);
        mh.invoke(clazz.newInstance(), request, response);
    }
}
