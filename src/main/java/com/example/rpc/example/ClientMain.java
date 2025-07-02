package com.example.rpc.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.client.RpcClient;

/**
 * 客户端启动类
 */
public class ClientMain {
    private static final Logger logger = LoggerFactory.getLogger(ClientMain.class);

    public static void main(String[] args) {
        // 创建 RPC 客户端
        RpcClient client = new RpcClient("localhost", 8080);

        // 创建服务代理
        HelloService helloService = client.createProxy(HelloService.class);

        try {
            // 测试各种方法调用
            logger.info("开始测试 RPC 调用...");

            // 测试 sayHello 方法
            String greeting = helloService.sayHello("World");
            logger.info("sayHello 返回结果: {}", greeting);

            // 测试 add 方法
            int sum = helloService.add(10, 20);
            logger.info("add(10, 20) 返回结果: {}", sum);

            // 测试 getCurrentTime 方法
            long currentTime = helloService.getCurrentTime();
            logger.info("getCurrentTime 返回结果: {}", currentTime);

            logger.info("所有测试完成！");

        } catch (Exception e) {
            logger.error("RPC 调用失败", e);
        }
    }
}
