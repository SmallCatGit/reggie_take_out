package com.reggie.common;

/**
 * 基于ThreadLocal封装的工具类，用于保存和获取当前登录用户的id
 * （作用范围：某个线程之内。每次请求都会开新的线程，所以不会混淆数据）
 */
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置值
     * @param id
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 获取值
     * @return
     */
    public static Long getThreadLocal() {
        return threadLocal.get();
    }
}
