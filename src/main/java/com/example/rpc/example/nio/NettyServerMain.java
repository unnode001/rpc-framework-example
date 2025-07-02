package com.example.rpc.example.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.example.HelloService;
import com.example.rpc.example.HelloServiceImpl;
import com.example.rpc.serialization.SerializerFactory;
import com.example.rpc.server.nio.NettyRpcServer;

/**
 * 基于 Netty 的服务端启动类
 */
public class NettyServerMain {
    private static final Logger logger = LoggerFactory.getLogger(NettyServerMain.class);

    public static void main(String[] args) {
        // 可以通过命令行参数指定序列化器
        String serializerName = args.length > 0 ? args[0] : "kryo";

        try {
            // 创建 Netty RPC 服务器
            NettyRpcServer server = new NettyRpcServer(8080, SerializerFactory.getSerializer(serializerName));

            // 注册服务实现
            server.registerService(HelloService.class, new HelloServiceImpl());

            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("正在关闭服务器...");
                server.stop();
            }));

            // 启动服务器
            logger.info("正在启动 Netty RPC 服务器 (序列化器: {})...", serializerName);
            server.start();

        } catch (Exception e) {
            logger.error("服务器启动失败", e);
        }
    }
}
