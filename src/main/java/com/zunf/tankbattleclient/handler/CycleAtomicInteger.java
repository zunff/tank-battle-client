package com.zunf.tankbattleclient.handler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 使用安全范围避免溢出：从 1 到 MAX_SAFE_REQUEST_ID
 * 预留足够大的缓冲空间，确保循环使用时不会与未完成的请求冲突
 * 假设最大超时时间为 60 秒，每秒最多 1000 条消息，需要 60000 个ID
 * 设置安全阈值为 MAX_VALUE - 100000，提供足够的安全余量
 */
public class CycleAtomicInteger {

    private static final int MAX_SAFE_REQUEST_ID = Integer.MAX_VALUE - 100_000;
    private static final int MIN_REQUEST_ID = 1;

    private final AtomicInteger requestIdAtomic = new AtomicInteger(MIN_REQUEST_ID);

    /**
     * 获取下一个请求ID，自动循环使用避免溢出
     *
     * 安全机制：
     * 1. 设置安全阈值 MAX_SAFE_REQUEST_ID = Integer.MAX_VALUE - 100000
     * 2. 当达到阈值时，循环回到 MIN_REQUEST_ID (1)
     * 3. 由于有超时机制（默认5秒），旧的请求ID在循环回来之前已经超时或处理完
     * 4. 100000 的缓冲空间足够大，即使有延迟也不会与未完成的请求冲突
     */
    public int getNextRequestId() {
        while (true) {
            int current = requestIdAtomic.get();

            // 如果当前值在安全范围内，直接递增并返回
            if (current <= MAX_SAFE_REQUEST_ID) {
                int next = requestIdAtomic.getAndIncrement();
                // 再次检查，防止在检查和递增之间被其他线程重置
                if (next <= MAX_SAFE_REQUEST_ID) {
                    return next;
                }
                // 如果被重置了，继续循环获取新值
                continue;
            }

            // 当前值超过安全阈值，尝试重置为起始值
            // 使用 CAS 操作确保线程安全，只有一个线程能成功重置
            if (requestIdAtomic.compareAndSet(current, MIN_REQUEST_ID)) {
                System.out.println("请求ID达到安全阈值 " + current + "，重置为 " + MIN_REQUEST_ID);
                return MIN_REQUEST_ID;
            }
            // 如果 CAS 失败，说明其他线程已经重置，继续循环获取新值
        }
    }
}
