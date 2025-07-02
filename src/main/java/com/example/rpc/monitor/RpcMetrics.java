package com.example.rpc.monitor;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * RPC 调用指标统计
 */
public class RpcMetrics {

    // 请求计数
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();

    // 响应时间统计
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong maxResponseTime = new AtomicLong(0);
    private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);

    // 活跃连接数
    private final LongAdder activeConnections = new LongAdder();

    // 错误统计
    private final LongAdder timeoutErrors = new LongAdder();
    private final LongAdder networkErrors = new LongAdder();
    private final LongAdder businessErrors = new LongAdder();

    /**
     * 记录请求开始
     */
    public void recordRequestStart() {
        totalRequests.increment();
        activeConnections.increment();
    }

    /**
     * 记录请求成功
     */
    public void recordRequestSuccess(long responseTime) {
        successRequests.increment();
        activeConnections.decrement();
        updateResponseTime(responseTime);
    }

    /**
     * 记录请求失败
     */
    public void recordRequestFailure(long responseTime, ErrorType errorType) {
        failedRequests.increment();
        activeConnections.decrement();
        updateResponseTime(responseTime);

        switch (errorType) {
            case TIMEOUT:
                timeoutErrors.increment();
                break;
            case NETWORK:
                networkErrors.increment();
                break;
            case BUSINESS:
                businessErrors.increment();
                break;
        }
    }

    /**
     * 更新响应时间统计
     */
    private void updateResponseTime(long responseTime) {
        totalResponseTime.addAndGet(responseTime);

        // 更新最大响应时间
        long currentMax = maxResponseTime.get();
        while (responseTime > currentMax) {
            if (maxResponseTime.compareAndSet(currentMax, responseTime)) {
                break;
            }
            currentMax = maxResponseTime.get();
        }

        // 更新最小响应时间
        long currentMin = minResponseTime.get();
        while (responseTime < currentMin) {
            if (minResponseTime.compareAndSet(currentMin, responseTime)) {
                break;
            }
            currentMin = minResponseTime.get();
        }
    }

    // 获取统计信息的方法

    public long getTotalRequests() {
        return totalRequests.sum();
    }

    public long getSuccessRequests() {
        return successRequests.sum();
    }

    public long getFailedRequests() {
        return failedRequests.sum();
    }

    public double getSuccessRate() {
        long total = getTotalRequests();
        return total > 0 ? (double) getSuccessRequests() / total * 100 : 0;
    }

    public long getActiveConnections() {
        return activeConnections.sum();
    }

    public double getAverageResponseTime() {
        long total = getTotalRequests();
        return total > 0 ? (double) totalResponseTime.get() / total : 0;
    }

    public long getMaxResponseTime() {
        return maxResponseTime.get();
    }

    public long getMinResponseTime() {
        long min = minResponseTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getTimeoutErrors() {
        return timeoutErrors.sum();
    }

    public long getNetworkErrors() {
        return networkErrors.sum();
    }

    public long getBusinessErrors() {
        return businessErrors.sum();
    }

    /**
     * 重置统计信息
     */
    public void reset() {
        totalRequests.reset();
        successRequests.reset();
        failedRequests.reset();
        totalResponseTime.set(0);
        maxResponseTime.set(0);
        minResponseTime.set(Long.MAX_VALUE);
        activeConnections.reset();
        timeoutErrors.reset();
        networkErrors.reset();
        businessErrors.reset();
    }

    /**
     * 获取格式化的统计信息
     */
    public String getFormattedStats() {
        return String.format(
                "RPC Metrics:\n" +
                        "  Total Requests: %d\n" +
                        "  Success Requests: %d\n" +
                        "  Failed Requests: %d\n" +
                        "  Success Rate: %.2f%%\n" +
                        "  Active Connections: %d\n" +
                        "  Average Response Time: %.2f ms\n" +
                        "  Max Response Time: %d ms\n" +
                        "  Min Response Time: %d ms\n" +
                        "  Timeout Errors: %d\n" +
                        "  Network Errors: %d\n" +
                        "  Business Errors: %d",
                getTotalRequests(),
                getSuccessRequests(),
                getFailedRequests(),
                getSuccessRate(),
                getActiveConnections(),
                getAverageResponseTime(),
                getMaxResponseTime(),
                getMinResponseTime(),
                getTimeoutErrors(),
                getNetworkErrors(),
                getBusinessErrors());
    }

    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        TIMEOUT, // 超时错误
        NETWORK, // 网络错误
        BUSINESS // 业务错误
    }
}
