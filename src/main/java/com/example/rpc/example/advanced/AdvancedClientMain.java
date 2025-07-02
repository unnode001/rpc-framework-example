package com.example.rpc.example.advanced;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.async.AsyncRpcClientProxy;
import com.example.rpc.client.IRpcClient;
import com.example.rpc.client.nio.DiscoveryRpcClient;
import com.example.rpc.config.RpcConfig;
import com.example.rpc.example.HelloService;
import com.example.rpc.loadbalancer.LoadBalancerFactory;
import com.example.rpc.monitor.RpcMonitor;
import com.example.rpc.registry.impl.NacosServiceRegistry;
import com.example.rpc.serialization.SerializerFactory;

/**
 * 第四阶段高级功能客户端示例
 * 展示异步调用、监控、容错等高级特性
 */
public class AdvancedClientMain {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedClientMain.class);

    public static void main(String[] args) throws Exception {
        // 1. 加载配置
        RpcConfig config = RpcConfig.getInstance();
        logger.info("加载配置完成");

        // 2. 创建注册中心
        NacosServiceRegistry serviceRegistry = new NacosServiceRegistry(config.getRegistryAddress());

        // 3. 创建客户端
        DiscoveryRpcClient client = new DiscoveryRpcClient(
                serviceRegistry,
                SerializerFactory.createSerializer(config.getSerializationType()),
                LoadBalancerFactory.createLoadBalancer(config.getLoadBalancerType()),
                config.getClientTimeout());

        // 4. 启动监控
        RpcMonitor monitor = RpcMonitor.getInstance();

        try {
            // 5. 测试同步调用
            testSyncCall(client, monitor);

            // 6. 测试异步调用
            testAsyncCall(client, monitor);

            // 7. 测试容错机制
            testFaultTolerance(client, monitor);

            // 8. 压力测试
            performanceTest(client, monitor);

            // 9. 等待一段时间查看监控报告
            Thread.sleep(65000);

        } finally {
            // 清理资源
            client.close();
            serviceRegistry.close();
            monitor.shutdown();
        }
    }

    /**
     * 测试同步调用
     */
    private static void testSyncCall(DiscoveryRpcClient client, RpcMonitor monitor) {
        logger.info("=== 测试同步调用 ===");

        HelloService helloService = client.createProxy(HelloService.class);

        try {
            long startTime = System.currentTimeMillis();
            monitor.recordRequestStart("HelloService");

            String result = helloService.sayHello("同步调用");
            long responseTime = System.currentTimeMillis() - startTime;

            monitor.recordRequestSuccess("HelloService", responseTime);
            logger.info("同步调用结果: {}, 响应时间: {} ms", result, responseTime);

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - System.currentTimeMillis();
            monitor.recordRequestFailure("HelloService", responseTime,
                    com.example.rpc.monitor.RpcMetrics.ErrorType.BUSINESS);
            logger.error("同步调用失败", e);
        }
    }

    /**
     * 测试异步调用
     */
    private static void testAsyncCall(DiscoveryRpcClient client, RpcMonitor monitor) {
        logger.info("=== 测试异步调用 ===");

        // 创建异步代理
        AsyncRpcClientProxy.AsyncServiceProxy<HelloService> asyncProxy = AsyncRpcClientProxy.create((IRpcClient) client,
                HelloService.class);

        try {
            long startTime = System.currentTimeMillis();
            monitor.recordRequestStart("HelloService");

            // 异步调用
            CompletableFuture<Object> future = asyncProxy.callAsync("sayHello", "异步调用");

            // 处理异步结果
            future.thenAccept(result -> {
                long responseTime = System.currentTimeMillis() - startTime;
                monitor.recordRequestSuccess("HelloService", responseTime);
                logger.info("异步调用结果: {}, 响应时间: {} ms", result, responseTime);
            }).exceptionally(throwable -> {
                long responseTime = System.currentTimeMillis() - startTime;
                monitor.recordRequestFailure("HelloService", responseTime,
                        com.example.rpc.monitor.RpcMetrics.ErrorType.BUSINESS);
                logger.error("异步调用失败", throwable);
                return null;
            });

            // 等待异步调用完成
            Thread.sleep(2000);

        } catch (Exception e) {
            logger.error("异步调用测试失败", e);
        }
    }

    /**
     * 测试容错机制
     */
    private static void testFaultTolerance(DiscoveryRpcClient client, RpcMonitor monitor) {
        logger.info("=== 测试容错机制 ===");

        // 这里可以模拟各种故障情况
        // 例如：网络超时、服务不可用等
        HelloService helloService = client.createProxy(HelloService.class);

        for (int i = 0; i < 5; i++) {
            try {
                long startTime = System.currentTimeMillis();
                monitor.recordRequestStart("HelloService");

                String result = helloService.sayHello("容错测试-" + i);
                long responseTime = System.currentTimeMillis() - startTime;

                monitor.recordRequestSuccess("HelloService", responseTime);
                logger.info("容错测试 {} 成功: {}", i, result);

            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - System.currentTimeMillis();
                monitor.recordRequestFailure("HelloService", responseTime,
                        com.example.rpc.monitor.RpcMetrics.ErrorType.NETWORK);
                logger.warn("容错测试 {} 失败: {}", i, e.getMessage());
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 性能测试
     */
    private static void performanceTest(DiscoveryRpcClient client, RpcMonitor monitor) {
        logger.info("=== 性能测试开始 ===");

        HelloService helloService = client.createProxy(HelloService.class);
        int totalRequests = 100;
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            try {
                long startTime = System.currentTimeMillis();
                monitor.recordRequestStart("HelloService");

                helloService.sayHello("性能测试-" + i);
                long responseTime = System.currentTimeMillis() - startTime;

                monitor.recordRequestSuccess("HelloService", responseTime);

                if (i % 20 == 0) {
                    logger.info("已完成 {} 个请求", i);
                }

            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - System.currentTimeMillis();
                monitor.recordRequestFailure("HelloService", responseTime,
                        com.example.rpc.monitor.RpcMetrics.ErrorType.BUSINESS);
                logger.warn("请求 {} 失败: {}", i, e.getMessage());
            }
        }

        long totalTime = System.currentTimeMillis() - testStartTime;
        double qps = (double) totalRequests / (totalTime / 1000.0);

        logger.info("=== 性能测试完成 ===");
        logger.info("总请求数: {}", totalRequests);
        logger.info("总耗时: {} ms", totalTime);
        logger.info("平均 QPS: {:.2f}", qps);

        // 打印详细统计信息
        monitor.printAllStats();
    }
}
