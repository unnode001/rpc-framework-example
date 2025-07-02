package com.example.rpc.example.discovery;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.loadbalancer.LoadBalancer;
import com.example.rpc.loadbalancer.LoadBalancerFactory;

/**
 * 负载均衡算法测试
 */
public class LoadBalancerTest {
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerTest.class);

    public static void main(String[] args) {
        // 模拟服务实例列表
        List<String> services = Arrays.asList(
                "192.168.1.100:8080",
                "192.168.1.101:8080",
                "192.168.1.102:8080",
                "192.168.1.103:8080");

        int testCount = 1000;
        logger.info("负载均衡算法测试");
        logger.info("服务实例: {}", services);
        logger.info("测试次数: {}", testCount);
        logger.info("=====================================");

        // 测试所有负载均衡算法
        String[] algorithms = LoadBalancerFactory.getSupportedLoadBalancers();

        for (String algorithm : algorithms) {
            testLoadBalancer(algorithm, services, testCount);
            logger.info("-------------------------------------");
        }
    }

    private static void testLoadBalancer(String algorithm, List<String> services, int testCount) {
        LoadBalancer loadBalancer = LoadBalancerFactory.getLoadBalancer(algorithm);
        Map<String, Integer> distribution = new HashMap<>();

        // 初始化计数器
        for (String service : services) {
            distribution.put(service, 0);
        }

        logger.info("测试负载均衡算法: {}", algorithm.toUpperCase());

        // 执行测试
        long startTime = System.nanoTime();
        for (int i = 0; i < testCount; i++) {
            String requestId = UUID.randomUUID().toString();
            String selected = loadBalancer.select(services, requestId);
            distribution.put(selected, distribution.get(selected) + 1);
        }
        long endTime = System.nanoTime();

        // 计算执行时间
        double avgTime = (endTime - startTime) / 1_000_000.0 / testCount;

        // 输出结果
        logger.info("平均选择耗时: {:.3f} ms", avgTime);
        logger.info("分布结果:");

        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            String service = entry.getKey();
            int count = entry.getValue();
            double percentage = (double) count / testCount * 100;
            logger.info("  {} -> {} 次 ({:.1f}%)", service, count, percentage);
        }

        // 计算分布均匀度 (标准差)
        double mean = (double) testCount / services.size();
        double variance = 0;
        for (int count : distribution.values()) {
            variance += Math.pow(count - mean, 2);
        }
        variance /= services.size();
        double stdDev = Math.sqrt(variance);
        double uniformity = (1 - stdDev / mean) * 100;

        logger.info("分布均匀度: {:.1f}% (越接近100%越均匀)", uniformity);
    }
}
