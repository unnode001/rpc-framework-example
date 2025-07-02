package com.example.rpc.example.advanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.config.RpcConfig;
import com.example.rpc.example.HelloService;
import com.example.rpc.example.HelloServiceImpl;
import com.example.rpc.registry.HealthChecker;
import com.example.rpc.registry.impl.NacosServiceRegistry;
import com.example.rpc.serialization.SerializerFactory;
import com.example.rpc.server.nio.NettyRpcServer;

/**
 * 第四阶段高级功能服务端示例
 * 展示配置管理、健康检查、监控等高级特性
 */
public class AdvancedServerMain {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedServerMain.class);

    public static void main(String[] args) throws Exception {
        // 1. 加载配置
        RpcConfig config = RpcConfig.getInstance();
        logger.info("加载配置完成");
        logger.info("服务端端口: {}", config.getServerPort());
        logger.info("序列化类型: {}", config.getSerializationType());
        logger.info("注册中心地址: {}", config.getRegistryAddress());

        // 2. 创建注册中心
        NacosServiceRegistry serviceRegistry = new NacosServiceRegistry(config.getRegistryAddress());

        // 3. 创建服务器
        NettyRpcServer server = new NettyRpcServer(
                config.getServerPort(),
                SerializerFactory.createSerializer(config.getSerializationType()),
                serviceRegistry);

        // 4. 注册服务
        server.registerService(HelloService.class, new HelloServiceImpl());
        logger.info("服务注册完成");

        // 5. 启动健康检查（如果启用）
        HealthChecker healthChecker = null;
        if (config.isHealthCheckEnabled()) {
            healthChecker = new HealthChecker(serviceRegistry, config.getHealthCheckInterval());
            healthChecker.startHealthCheck();
            logger.info("健康检查已启动");
        }

        // 6. 添加关闭钩子
        final HealthChecker finalHealthChecker = healthChecker;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("正在关闭服务器...");

            if (finalHealthChecker != null) {
                finalHealthChecker.stopHealthCheck();
            }

            server.stop();
            serviceRegistry.close();

            logger.info("服务器已关闭");
        }, "shutdown-hook"));

        // 7. 启动服务器
        logger.info("=== 启动高级 RPC 服务器 ===");
        logger.info("监听端口: {}", config.getServerPort());
        logger.info("序列化器: {}", config.getSerializationType());
        logger.info("注册中心: {}", config.getRegistryAddress());
        logger.info("健康检查: {}", config.isHealthCheckEnabled() ? "启用" : "禁用");

        try {
            server.start();
        } catch (Exception e) {
            logger.error("服务器启动失败", e);
            System.exit(1);
        }
    }
}
