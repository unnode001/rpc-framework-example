package com.example.rpc.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.client.nio.NettyRpcClient;
import com.example.rpc.example.HelloService;
import com.example.rpc.example.HelloServiceImpl;
import com.example.rpc.serialization.SerializerFactory;
import com.example.rpc.server.nio.NettyRpcServer;

/**
 * 一体化演示 - 在同一个程序中启动服务器和客户端
 */
public class AllInOneDemo {
    private static final Logger logger = LoggerFactory.getLogger(AllInOneDemo.class);

    public static void main(String[] args) throws Exception {
        logger.info("=== RPC 框架一体化演示开始 ===");

        // 1. 启动服务器
        NettyRpcServer server = new NettyRpcServer(8081, SerializerFactory.getSerializer("kryo"));
        server.registerService(HelloService.class, new HelloServiceImpl());

        // 异步启动服务器
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                logger.error("服务器启动失败", e);
            }
        }, "server-thread");
        serverThread.start();

        // 等待服务器启动
        Thread.sleep(2000);
        logger.info("服务器启动完成，开始客户端测试...");

        // 2. 创建客户端并测试
        NettyRpcClient client = new NettyRpcClient("localhost", 8081, SerializerFactory.getSerializer("kryo"));

        try {
            // 连接到服务器
            client.connect();
            HelloService helloService = client.createProxy(HelloService.class);

            // 测试基本功能
            logger.info("--- 基本功能测试 ---");

            String result1 = helloService.sayHello("演示");
            logger.info("sayHello('演示') = {}", result1);

            int result2 = helloService.add(100, 200);
            logger.info("add(100, 200) = {}", result2);

            long result3 = helloService.getCurrentTime();
            logger.info("getCurrentTime() = {}", result3);

            // 测试性能
            logger.info("--- 性能测试 ---");
            int testCount = 100;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < testCount; i++) {
                helloService.add(i, i + 1);
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            double qps = (double) testCount / totalTime * 1000;

            logger.info("完成 {} 次调用，总耗时: {} ms，QPS: {:.2f}", testCount, totalTime, qps);

            logger.info("--- 演示完成 ---");

        } catch (Exception e) {
            logger.error("客户端测试失败", e);
        } finally {
            // 关闭客户端
            try {
                client.getClass().getMethod("close").invoke(client);
            } catch (Exception e) {
                logger.warn("关闭客户端失败", e);
            }

            // 关闭服务器
            server.stop();
        }

        logger.info("=== RPC 框架一体化演示结束 ===");
    }
}
