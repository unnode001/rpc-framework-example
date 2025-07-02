package com.example.rpc.example.advanced;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.async.AsyncRpcClientProxy;
import com.example.rpc.client.IRpcClient;
import com.example.rpc.client.nio.DiscoveryRpcClient;
import com.example.rpc.config.RpcConfig;
import com.example.rpc.example.HelloService;
import com.example.rpc.example.HelloServiceImpl;
import com.example.rpc.loadbalancer.LoadBalancerFactory;
import com.example.rpc.monitor.RpcMonitor;
import com.example.rpc.registry.impl.NacosServiceRegistry;
import com.example.rpc.serialization.SerializerFactory;
import com.example.rpc.server.nio.NettyRpcServer;

/**
 * RPC 框架综合功能测试
 */
public class ComprehensiveTest {
    private static final Logger logger = LoggerFactory.getLogger(ComprehensiveTest.class);

    public static void main(String[] args) throws Exception {
        logger.info("=== RPC 框架综合功能测试开始 ===");

        // 启动服务端
        NettyRpcServer server = startServer();

        // 等待服务端启动
        Thread.sleep(3000);

        // 运行客户端测试
        runClientTests();

        // 关闭服务端
        server.stop();

        logger.info("=== RPC 框架综合功能测试完成 ===");
    }

    /**
     * 启动服务端
     */
    private static NettyRpcServer startServer() {
        logger.info("启动测试服务端...");

        RpcConfig config = RpcConfig.getInstance();
        NacosServiceRegistry serviceRegistry = new NacosServiceRegistry(config.getRegistryAddress());

        NettyRpcServer server = new NettyRpcServer(
                8081, // 使用不同端口避免冲突
                SerializerFactory.createSerializer("kryo"),
                serviceRegistry);

        server.registerService(HelloService.class, new HelloServiceImpl());

        // 异步启动服务器
        server.startAsync();

        return server;
    }

    /**
     * 运行客户端测试
     */
    private static void runClientTests() throws Exception {
        RpcConfig config = RpcConfig.getInstance();
        NacosServiceRegistry serviceRegistry = new NacosServiceRegistry(config.getRegistryAddress());

        DiscoveryRpcClient client = new DiscoveryRpcClient(
                serviceRegistry,
                SerializerFactory.createSerializer("kryo"),
                LoadBalancerFactory.createLoadBalancer("random"),
                5000);

        RpcMonitor monitor = RpcMonitor.getInstance();

        try {
            // 1. 基础功能测试
            testBasicFunction(client, monitor);

            // 2. 异步调用测试
            testAsyncCall(client, monitor);

            // 3. 负载均衡测试
            testLoadBalance(client, monitor);

            // 4. 序列化性能测试
            testSerializationPerformance(client, monitor);

            // 5. 并发测试
            testConcurrency(client, monitor);

            // 等待监控报告
            Thread.sleep(5000);
            monitor.printAllStats();

        } finally {
            client.close();
            serviceRegistry.close();
            monitor.shutdown();
        }
    }

    /**
     * 基础功能测试
     */
    private static void testBasicFunction(IRpcClient client, RpcMonitor monitor) throws Exception {
        logger.info("--- 基础功能测试 ---");

        HelloService helloService = client.createProxy(HelloService.class);

        for (int i = 0; i < 10; i++) {
            try {
                long startTime = System.currentTimeMillis();
                monitor.recordRequestStart("HelloService");

                String result = helloService.sayHello("测试" + i);
                long responseTime = System.currentTimeMillis() - startTime;

                monitor.recordRequestSuccess("HelloService", responseTime);
                logger.info("调用结果: {}, 响应时间: {} ms", result, responseTime);

            } catch (Exception e) {
                monitor.recordRequestFailure("HelloService", 0,
                        com.example.rpc.monitor.RpcMetrics.ErrorType.BUSINESS);
                logger.error("调用失败", e);
            }
        }
    }

    /**
     * 异步调用测试
     */
    private static void testAsyncCall(IRpcClient client, RpcMonitor monitor) throws Exception {
        logger.info("--- 异步调用测试 ---");

        AsyncRpcClientProxy.AsyncServiceProxy<HelloService> asyncProxy = AsyncRpcClientProxy.create(client,
                HelloService.class);

        @SuppressWarnings("unchecked")
        CompletableFuture<Object>[] futures = new CompletableFuture[5];

        for (int i = 0; i < 5; i++) {
            final int index = i;
            long startTime = System.currentTimeMillis();
            monitor.recordRequestStart("HelloService");

            futures[i] = asyncProxy.callAsync("sayHello", "异步" + i)
                    .thenApply(result -> {
                        long responseTime = System.currentTimeMillis() - startTime;
                        monitor.recordRequestSuccess("HelloService", responseTime);
                        logger.info("异步调用 {} 结果: {}, 响应时间: {} ms", index, result, responseTime);
                        return result;
                    })
                    .exceptionally(throwable -> {
                        monitor.recordRequestFailure("HelloService", 0,
                                com.example.rpc.monitor.RpcMetrics.ErrorType.BUSINESS);
                        logger.error("异步调用 {} 失败", index, throwable);
                        return null;
                    });
        }

        // 等待所有异步调用完成
        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
    }

    /**
     * 负载均衡测试
     */
    private static void testLoadBalance(IRpcClient client, RpcMonitor monitor) throws Exception {
        logger.info("--- 负载均衡测试 ---");

        HelloService helloService = client.createProxy(HelloService.class);

        // 快速连续调用，观察负载均衡效果
        for (int i = 0; i < 20; i++) {
            try {
                long startTime = System.currentTimeMillis();
                monitor.recordRequestStart("HelloService");

                helloService.sayHello("负载测试" + i);
                long responseTime = System.currentTimeMillis() - startTime;

                monitor.recordRequestSuccess("HelloService", responseTime);

                if (i % 5 == 0) {
                    logger.info("负载均衡测试进度: {}/20", i);
                }

            } catch (Exception e) {
                monitor.recordRequestFailure("HelloService", 0,
                        com.example.rpc.monitor.RpcMetrics.ErrorType.BUSINESS);
                logger.error("负载均衡测试失败", e);
            }

            Thread.sleep(100); // 小延迟
        }
    }

    /**
     * 序列化性能测试
     */
    private static void testSerializationPerformance(IRpcClient client, RpcMonitor monitor) throws Exception {
        logger.info("--- 序列化性能测试 ---");

        HelloService helloService = client.createProxy(HelloService.class);

        long totalTime = 0;
        int testCount = 50;

        for (int i = 0; i < testCount; i++) {
            try {
                long startTime = System.currentTimeMillis();
                monitor.recordRequestStart("HelloService");

                // 测试不同大小的参数
                StringBuilder sb = new StringBuilder("大参数测试");
                for (int j = 0; j < i % 10 + 1; j++) {
                    sb.append("大参数测试");
                }
                String largeParam = sb.toString();
                helloService.sayHello(largeParam);

                long responseTime = System.currentTimeMillis() - startTime;
                totalTime += responseTime;

                monitor.recordRequestSuccess("HelloService", responseTime);

            } catch (Exception e) {
                monitor.recordRequestFailure("HelloService", 0,
                        com.example.rpc.monitor.RpcMetrics.ErrorType.BUSINESS);
                logger.error("序列化测试失败", e);
            }
        }

        double avgTime = (double) totalTime / testCount;
        logger.info("序列化性能测试完成，平均响应时间: {:.2f} ms", avgTime);
    }

    /**
     * 并发测试
     */
    private static void testConcurrency(IRpcClient client, RpcMonitor monitor) throws Exception {
        logger.info("--- 并发测试 ---");

        HelloService helloService = client.createProxy(HelloService.class);
        int threadCount = 10;
        int requestsPerThread = 5;

        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            futures[t] = CompletableFuture.runAsync(() -> {
                for (int i = 0; i < requestsPerThread; i++) {
                    try {
                        long startTime = System.currentTimeMillis();
                        monitor.recordRequestStart("HelloService");

                        String result = helloService.sayHello("并发测试-线程" + threadId + "-请求" + i);
                        long responseTime = System.currentTimeMillis() - startTime;

                        monitor.recordRequestSuccess("HelloService", responseTime);
                        logger.debug("线程{} 请求{} 完成: {}", threadId, i, result);

                    } catch (Exception e) {
                        monitor.recordRequestFailure("HelloService", 0,
                                com.example.rpc.monitor.RpcMetrics.ErrorType.BUSINESS);
                        logger.error("线程{} 请求{} 失败", threadId, i, e);
                    }
                }
            });
        }

        // 等待所有并发任务完成
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        logger.info("并发测试完成");
    }
}
