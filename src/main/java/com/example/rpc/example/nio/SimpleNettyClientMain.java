package com.example.rpc.example.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.client.nio.NettyRpcClient;
import com.example.rpc.example.HelloService;
import com.example.rpc.serialization.SerializerFactory;

/**
 * 简化版 Netty 客户端测试
 */
public class SimpleNettyClientMain {
    private static final Logger logger = LoggerFactory.getLogger(SimpleNettyClientMain.class);

    public static void main(String[] args) {
        NettyRpcClient client = null;
        try {
            // 创建 Netty RPC 客户端
            client = new NettyRpcClient("localhost", 8080, SerializerFactory.getSerializer("kryo"));

            // 连接到服务器
            client.connect();

            // 创建服务代理
            HelloService helloService = client.createProxy(HelloService.class);

            logger.info("开始简化测试...");

            // 测试基本功能
            String greeting = helloService.sayHello("Simple Test");
            logger.info("sayHello 返回结果: {}", greeting);

            int sum = helloService.add(5, 10);
            logger.info("add(5, 10) 返回结果: {}", sum);

            long currentTime = helloService.getCurrentTime();
            logger.info("getCurrentTime 返回结果: {}", currentTime);

            logger.info("简化测试完成！");

        } catch (Exception e) {
            logger.error("RPC 调用失败", e);
        } finally {
            if (client != null) {
                client.disconnect();
            }
        }
    }
}
