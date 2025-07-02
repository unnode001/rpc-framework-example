package com.example.rpc.server.nio;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;
import com.example.rpc.registry.ServiceRegistry;
import com.example.rpc.registry.impl.NacosServiceRegistry;
import com.example.rpc.serialization.Serializer;
import com.example.rpc.serialization.SerializerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * 基于 Netty 的 NIO RPC 服务器
 */
public class NettyRpcServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyRpcServer.class);

    private final int port;
    private final ConcurrentHashMap<String, Object> serviceMap = new ConcurrentHashMap<>();
    private final ExecutorService businessExecutor = Executors.newCachedThreadPool();
    private final Serializer serializer;
    private final ServiceRegistry serviceRegistry;
    private final String host;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public NettyRpcServer(int port) {
        this(port, SerializerFactory.getDefaultSerializer(), null);
    }

    public NettyRpcServer(int port, Serializer serializer) {
        this(port, serializer, null);
    }

    public NettyRpcServer(int port, Serializer serializer, ServiceRegistry serviceRegistry) {
        this.port = port;
        this.serializer = serializer;
        this.serviceRegistry = serviceRegistry;
        this.host = NacosServiceRegistry.getLocalHost();
    }

    /**
     * 注册服务实现
     */
    public void registerService(Class<?> serviceInterface, Object serviceImpl) {
        String serviceName = serviceInterface.getName();
        serviceMap.put(serviceName, serviceImpl);
        logger.info("注册服务: {} (序列化器: {})", serviceName, serializer.getName());

        // 如果有注册中心，则注册到注册中心
        if (serviceRegistry != null) {
            String serviceAddress = host + ":" + port;
            serviceRegistry.registerService(serviceName, serviceAddress);
        }
    }

    /**
     * 启动服务器
     */
    public void start() {
        bossGroup = new NioEventLoopGroup(1); // 用于接受连接
        workerGroup = new NioEventLoopGroup(); // 用于处理I/O

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // 添加长度字段解码器，解决TCP粘包/拆包问题
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));

                            // 添加自定义编解码器
                            pipeline.addLast(new RpcRequestDecoder(serializer));
                            pipeline.addLast(new RpcResponseEncoder(serializer));

                            // 添加业务处理器
                            pipeline.addLast(new RpcServerHandler(serviceMap, businessExecutor));
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            logger.info("Netty RPC 服务器启动，监听端口: {} (序列化器: {})", port, serializer.getName());

            // 等待服务器关闭
            future.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            logger.error("服务器启动失败", e);
            Thread.currentThread().interrupt();
        } finally {
            stop();
        }
    }

    /**
     * 异步启动服务器
     */
    public void startAsync() {
        new Thread(this::start, "NettyRpcServer").start();
    }

    /**
     * 停止服务器
     */
    public void stop() {
        try {
            // 注销注册中心中的服务
            if (serviceRegistry != null) {
                String serviceAddress = host + ":" + port;
                for (String serviceName : serviceMap.keySet()) {
                    serviceRegistry.unregisterService(serviceName, serviceAddress);
                }
                serviceRegistry.close();
            }

            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (InterruptedException e) {
            logger.error("关闭服务器通道失败", e);
            Thread.currentThread().interrupt();
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            businessExecutor.shutdown();
            logger.info("Netty RPC 服务器已停止");
        }
    }

    /**
     * RPC 服务器处理器
     */
    private static class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
        private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);

        private final ConcurrentHashMap<String, Object> serviceMap;
        private final ExecutorService businessExecutor;

        public RpcServerHandler(ConcurrentHashMap<String, Object> serviceMap, ExecutorService businessExecutor) {
            this.serviceMap = serviceMap;
            this.businessExecutor = businessExecutor;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) {
            logger.info("接收到请求: {}", request);

            // 将业务处理提交到业务线程池，避免阻塞I/O线程
            businessExecutor.submit(() -> {
                RpcResponse response = handleRequest(request);
                logger.info("处理完成，返回响应: {}", response);
                ctx.writeAndFlush(response);
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("处理请求时发生异常", cause);
            ctx.close();
        }

        private RpcResponse handleRequest(RpcRequest request) {
            try {
                // 获取服务实现对象
                Object serviceImpl = serviceMap.get(request.getInterfaceName());
                if (serviceImpl == null) {
                    throw new RuntimeException("未找到服务实现: " + request.getInterfaceName());
                }

                // 通过反射调用方法
                Method method = serviceImpl.getClass().getMethod(
                        request.getMethodName(),
                        request.getParamTypes());

                Object result = method.invoke(serviceImpl, request.getParams());

                return new RpcResponse(request.getRequestId(), result);

            } catch (Exception e) {
                logger.error("处理请求失败: {}", request, e);
                return new RpcResponse(request.getRequestId(), e);
            }
        }
    }
}
