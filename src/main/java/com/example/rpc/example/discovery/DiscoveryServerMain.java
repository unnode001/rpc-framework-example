package com.example.rpc.example.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.example.HelloService;
import com.example.rpc.example.HelloServiceImpl;
import com.example.rpc.registry.impl.NacosServiceRegistry;
import com.example.rpc.serialization.SerializerFactory;
import com.example.rpc.server.nio.NettyRpcServer;

/**
 * 基于服务发现的服务端启动类
 */
public class DiscoveryServerMain {
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryServerMain.class);

    public static void main(String[] args) {
        // 解析命令行参数
        String nacosAddr = args.length > 0 ? args[0] : "localhost:8848";
        String serializerName = args.length > 1 ? args[1] : "kryo";
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 8080;

        try {
            // 创建 Nacos 注册中心
            NacosServiceRegistry serviceRegistry = new NacosServiceRegistry(nacosAddr);

            // 创建 Netty RPC 服务器
            NettyRpcServer server = new NettyRpcServer(
                    port,
                    SerializerFactory.getSerializer(serializerName),
                    serviceRegistry);

            // 注册服务实现
            server.registerService(HelloService.class, new HelloServiceImpl());

            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("正在关闭服务器...");
                server.stop();
            }));

            // 启动服务器
            logger.info("正在启动支持服务发现的 RPC 服务器...");
            logger.info("Nacos 地址: {}", nacosAddr);
            logger.info("序列化器: {}", serializerName);
            logger.info("监听端口: {}", port);

            server.start();

        } catch (Exception e) {
            logger.error("服务器启动失败", e);
        }
    }
}
