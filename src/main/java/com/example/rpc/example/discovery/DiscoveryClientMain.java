package com.example.rpc.example.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.client.nio.DiscoveryRpcClient;
import com.example.rpc.example.HelloService;
import com.example.rpc.loadbalancer.LoadBalancerFactory;
import com.example.rpc.registry.impl.NacosServiceRegistry;
import com.example.rpc.serialization.SerializerFactory;

/**
 * 基于服务发现的客户端启动类
 */
public class DiscoveryClientMain {
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryClientMain.class);

    public static void main(String[] args) {
        // 解析命令行参数
        String nacosAddr = args.length > 0 ? args[0] : "localhost:8848";
        String serializerName = args.length > 1 ? args[1] : "kryo";
        String loadBalancerName = args.length > 2 ? args[2] : "roundrobin";

        DiscoveryRpcClient client = null;
        try {
            // 创建 Nacos 注册中心
            NacosServiceRegistry serviceRegistry = new NacosServiceRegistry(nacosAddr);

            // 创建支持服务发现的 RPC 客户端
            client = new DiscoveryRpcClient(
                    serviceRegistry,
                    SerializerFactory.getSerializer(serializerName),
                    LoadBalancerFactory.getLoadBalancer(loadBalancerName));

            // 创建服务代理
            HelloService helloService = client.createProxy(HelloService.class);

            // 测试基本功能
            logger.info("开始测试支持服务发现的 RPC 调用...");
            logger.info("Nacos 地址: {}", nacosAddr);
            logger.info("序列化器: {}", serializerName);
            logger.info("负载均衡: {}", loadBalancerName);

            // 测试 sayHello 方法
            String greeting = helloService.sayHello("Service Discovery World");
            logger.info("sayHello 返回结果: {}", greeting);

            // 测试 add 方法
            int sum = helloService.add(100, 200);
            logger.info("add(100, 200) 返回结果: {}", sum);

            // 测试 getCurrentTime 方法
            long currentTime = helloService.getCurrentTime();
            logger.info("getCurrentTime 返回结果: {}", currentTime);

            // 负载均衡测试 - 多次调用观察负载分配
            logger.info("开始负载均衡测试 (10次调用)...");
            for (int i = 0; i < 10; i++) {
                int result = helloService.add(i, i + 1);
                logger.info("第{}次调用 add({}, {}) = {}", i + 1, i, i + 1, result);
                Thread.sleep(500); // 间隔500ms
            }

            // 性能测试
            logger.info("开始性能测试...");
            long startTime = System.currentTimeMillis();
            int testCount = 100; // 减少测试次数，因为有网络开销

            for (int i = 0; i < testCount; i++) {
                helloService.add(i, i + 1);
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            double avgTime = (double) totalTime / testCount;

            logger.info("性能测试完成: {} 次调用耗时 {} ms, 平均每次 {} ms, QPS: {}",
                    testCount, totalTime, String.format("%.2f", avgTime),
                    String.format("%.2f", testCount * 1000.0 / totalTime));

            logger.info("所有测试完成！");

        } catch (Exception e) {
            logger.error("RPC 调用失败", e);
        } finally {
            if (client != null) {
                client.disconnect();
            }
        }
    }
}
