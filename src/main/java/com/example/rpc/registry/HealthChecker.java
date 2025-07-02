package com.example.rpc.registry;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 健康检查器，用于检测服务实例的健康状态
 */
public class HealthChecker {
    private static final Logger logger = LoggerFactory.getLogger(HealthChecker.class);

    private final ServiceRegistry serviceRegistry;
    private final ScheduledExecutorService scheduler;
    private final int checkInterval; // 检查间隔，单位：秒

    public HealthChecker(ServiceRegistry serviceRegistry) {
        this(serviceRegistry, 30); // 默认30秒检查一次
    }

    public HealthChecker(ServiceRegistry serviceRegistry, int checkInterval) {
        this.serviceRegistry = serviceRegistry;
        this.checkInterval = checkInterval;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * 开始健康检查
     */
    public void startHealthCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performHealthCheck();
            } catch (Exception e) {
                logger.error("健康检查失败", e);
            }
        }, checkInterval, checkInterval, TimeUnit.SECONDS);

        logger.info("健康检查器已启动，检查间隔: {} 秒", checkInterval);
    }

    /**
     * 停止健康检查
     */
    public void stopHealthCheck() {
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
        logger.info("健康检查器已停止");
    }

    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        // 获取所有注册的服务
        List<String> services = serviceRegistry.getAllServices();

        for (String serviceName : services) {
            List<String> instances = serviceRegistry.getServiceInstances(serviceName);

            for (String instance : instances) {
                if (!isHealthy(instance)) {
                    logger.warn("服务实例不健康，准备移除: {} - {}", serviceName, instance);
                    serviceRegistry.unregisterService(serviceName, instance);
                }
            }
        }
    }

    /**
     * 检查服务实例是否健康
     */
    private boolean isHealthy(String instance) {
        try {
            // 这里可以实现具体的健康检查逻辑
            // 比如发送HTTP请求、TCP连接测试等
            String[] parts = instance.split(":");
            if (parts.length != 2) {
                return false;
            }

            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            // 简单的TCP连接测试
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 3000);
                return true;
            }
        } catch (Exception e) {
            logger.debug("健康检查失败: {} - {}", instance, e.getMessage());
            return false;
        }
    }
}
