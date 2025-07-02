package com.example.rpc.example.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.client.nio.NettyRpcClient;
import com.example.rpc.example.HelloService;
import com.example.rpc.serialization.SerializerFactory;

/**
 * 基于 Netty 的客户端启动类
 */
public class NettyClientMain {
    private static final Logger logger = LoggerFactory.getLogger(NettyClientMain.class);

    public static void main(String[] args) {
        // 可以通过命令行参数指定序列化器
        String serializerName = args.length > 0 ? args[0] : "kryo";

        NettyRpcClient client = null;
        try {
            // 创建 Netty RPC 客户端
            client = new NettyRpcClient("localhost", 8080, SerializerFactory.getSerializer(serializerName));

            // 连接到服务器
            client.connect();

            // 创建服务代理
            HelloService helloService = client.createProxy(HelloService.class);

            // 测试各种方法调用
            logger.info("开始测试 Netty RPC 调用 (序列化器: {})...", serializerName);

            // 测试 sayHello 方法
            String greeting = helloService.sayHello("Netty World");
            logger.info("sayHello 返回结果: {}", greeting);

            // 测试 add 方法
            int sum = helloService.add(100, 200);
            logger.info("add(100, 200) 返回结果: {}", sum);

            // 测试 getCurrentTime 方法
            long currentTime = helloService.getCurrentTime();
            logger.info("getCurrentTime 返回结果: {}", currentTime);

            // 性能测试
            logger.info("开始性能测试...");
            long startTime = System.currentTimeMillis();
            int testCount = 1000;

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
