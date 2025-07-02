package com.example.rpc.monitor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC 监控管理器
 */
public class RpcMonitor {
    private static final Logger logger = LoggerFactory.getLogger(RpcMonitor.class);

    private static volatile RpcMonitor instance;
    private final ConcurrentHashMap<String, RpcMetrics> serviceMetrics;
    private final ScheduledExecutorService scheduler;
    private final boolean enableLogging;
    private final int reportInterval;

    private RpcMonitor() {
        this(true, 60); // 默认启用日志，60秒报告间隔
    }

    private RpcMonitor(boolean enableLogging, int reportInterval) {
        this.serviceMetrics = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.enableLogging = enableLogging;
        this.reportInterval = reportInterval;

        if (enableLogging) {
            startPeriodicReporting();
        }
    }

    public static RpcMonitor getInstance() {
        if (instance == null) {
            synchronized (RpcMonitor.class) {
                if (instance == null) {
                    instance = new RpcMonitor();
                }
            }
        }
        return instance;
    }

    /**
     * 获取服务的指标对象
     */
    public RpcMetrics getMetrics(String serviceName) {
        return serviceMetrics.computeIfAbsent(serviceName, k -> new RpcMetrics());
    }

    /**
     * 记录请求开始
     */
    public void recordRequestStart(String serviceName) {
        getMetrics(serviceName).recordRequestStart();
    }

    /**
     * 记录请求成功
     */
    public void recordRequestSuccess(String serviceName, long responseTime) {
        getMetrics(serviceName).recordRequestSuccess(responseTime);
    }

    /**
     * 记录请求失败
     */
    public void recordRequestFailure(String serviceName, long responseTime, RpcMetrics.ErrorType errorType) {
        getMetrics(serviceName).recordRequestFailure(responseTime, errorType);
    }

    /**
     * 获取所有服务的统计信息
     */
    public void printAllStats() {
        if (serviceMetrics.isEmpty()) {
            logger.info("暂无 RPC 调用统计信息");
            return;
        }

        logger.info("=== RPC 监控统计报告 ===");
        serviceMetrics.forEach((serviceName, metrics) -> {
            logger.info("服务: {}", serviceName);
            logger.info(metrics.getFormattedStats());
            logger.info("========================");
        });
    }

    /**
     * 重置所有统计信息
     */
    public void resetAllStats() {
        serviceMetrics.values().forEach(RpcMetrics::reset);
        logger.info("已重置所有 RPC 统计信息");
    }

    /**
     * 移除服务监控
     */
    public void removeService(String serviceName) {
        serviceMetrics.remove(serviceName);
        logger.info("已移除服务监控: {}", serviceName);
    }

    /**
     * 开始定期报告
     */
    private void startPeriodicReporting() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                printAllStats();
            } catch (Exception e) {
                logger.error("定期报告失败", e);
            }
        }, reportInterval, reportInterval, TimeUnit.SECONDS);

        logger.info("RPC 监控已启动，报告间隔: {} 秒", reportInterval);
    }

    /**
     * 关闭监控
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("RPC 监控已关闭");
    }

    /**
     * 获取服务QPS（每秒请求数）
     */
    public double getQPS(String serviceName, int periodSeconds) {
        RpcMetrics metrics = serviceMetrics.get(serviceName);
        if (metrics == null) {
            return 0;
        }

        // 这里简化处理，实际应该基于时间窗口计算
        return (double) metrics.getTotalRequests() / periodSeconds;
    }

    /**
     * 检查服务健康状态
     */
    public boolean isServiceHealthy(String serviceName, double minSuccessRate) {
        RpcMetrics metrics = serviceMetrics.get(serviceName);
        if (metrics == null) {
            return true; // 没有调用记录，认为是健康的
        }

        return metrics.getSuccessRate() >= minSuccessRate;
    }
}
