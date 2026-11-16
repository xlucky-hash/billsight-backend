package com.ldj.common.utlis;

import org.springframework.stereotype.Component;

@Component
public class CurrentHolder {
    private static final ThreadLocal<Integer> THREAD_LOCAL =new ThreadLocal<>();

    public static Integer getCurrentUserId()
    {
        return THREAD_LOCAL.get();
    }

    public static void setThreadLocal(Integer value)
    {
        THREAD_LOCAL.set(value);
    }

    public static void removeThreadLocal()
    {
        THREAD_LOCAL.remove();
    }
}
