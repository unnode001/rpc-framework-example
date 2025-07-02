package com.example.rpc.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.server.RpcServer;

/**
 * 服务端启动类
 */
public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {
        // 创建 RPC 服务器
        RpcServer server = new RpcServer(8080);

        // 注册服务实现
        server.registerService(HelloService.class, new HelloServiceImpl());

        // 启动服务器
        logger.info("正在启动 RPC 服务器...");
        server.start();
    }
}
