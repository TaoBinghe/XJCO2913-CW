package com.greengo.utils;

import java.util.HashMap;
import java.util.Map;

// ThreadLocal for current request (e.g. JWT claims)
@SuppressWarnings("all")
public class ThreadLocalUtil {

    private static final ThreadLocal THREAD_LOCAL = new ThreadLocal();

    // Get value in current thread
    public static <T> T get(){
        return (T) THREAD_LOCAL.get();
    }

    // Set value for current thread
    public static void set(Object value){
        THREAD_LOCAL.set(value);
    }

    // Clear after request
    public static void remove(){
        THREAD_LOCAL.remove();
    }
}

