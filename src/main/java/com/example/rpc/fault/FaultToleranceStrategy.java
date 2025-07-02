package com.example.rpc.fault;

/**
 * 容错策略枚举
 */
public enum FaultToleranceStrategy {
    /**
     * 快速失败：调用失败后立即抛出异常
     */
    FAIL_FAST,

    /**
     * 失败重试：调用失败后进行重试
     */
    FAIL_RETRY,

    /**
     * 失败转移：调用失败后尝试其他服务实例
     */
    FAIL_OVER,

    /**
     * 失败安全：调用失败后返回默认值，不抛出异常
     */
    FAIL_SAFE,

    /**
     * 失败回退：调用失败后返回之前的缓存值
     */
    FAIL_BACK
}
