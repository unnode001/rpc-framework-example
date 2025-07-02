package com.example.rpc.client.nio;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;
import com.example.rpc.serialization.Serializer;
import com.example.rpc.serialization.SerializerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * 基于 Netty 的 NIO RPC 客户端
 */
public class NettyRpcClient {
    private static final Logger logger = LoggerFactory.getLogger(NettyRpcClient.class);

    private final String host;
    private final int port;
    private final Serializer serializer;

    private EventLoopGroup eventLoopGroup;
    private Channel channel;
    private final ConcurrentHashMap<String, CompletableFuture<RpcResponse>> pendingRequests = new ConcurrentHashMap<>();

    public NettyRpcClient(String host, int port) {
        this(host, port, SerializerFactory.getDefaultSerializer());
    }

    public NettyRpcClient(String host, int port, Serializer serializer) {
        this.host = host;
        this.port = port;
        this.serializer = serializer;
    }

    /**
     * 连接到服务器
     */
    public void connect() throws InterruptedException {
        eventLoopGroup = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 添加长度字段解码器，解决TCP粘包/拆包问题
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                        pipeline.addLast(new LengthFieldPrepender(4));

                        // 添加自定义编解码器
                        pipeline.addLast(new RpcRequestEncoder(serializer));
                        pipeline.addLast(new RpcResponseDecoder(serializer));

                        // 添加客户端处理器
                        pipeline.addLast(new RpcClientHandler(pendingRequests));
                    }
                });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();
        logger.info("连接到 RPC 服务器: {}:{} (序列化器: {})", host, port, serializer.getName());
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            if (channel != null) {
                channel.close().sync();
            }
        } catch (InterruptedException e) {
            logger.error("关闭连接失败", e);
            Thread.currentThread().interrupt();
        } finally {
            if (eventLoopGroup != null) {
                eventLoopGroup.shutdownGracefully();
            }
            logger.info("已断开与 RPC 服务器的连接");
        }
    }

    /**
     * 创建服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> serviceInterface) {
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class[] { serviceInterface },
                new ServiceInvocationHandler(serviceInterface));
    }

    /**
     * 发送RPC请求
     */
    private CompletableFuture<RpcResponse> sendRequest(RpcRequest request) {
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pendingRequests.put(request.getRequestId(), future);

        channel.writeAndFlush(request).addListener((ChannelFutureListener) channelFuture -> {
            if (!channelFuture.isSuccess()) {
                pendingRequests.remove(request.getRequestId());
                future.completeExceptionally(new RuntimeException("发送请求失败", channelFuture.cause()));
            }
        });

        return future;
    }

    /**
     * 服务调用处理器
     */
    private class ServiceInvocationHandler implements InvocationHandler {
        private final Class<?> serviceInterface;

        public ServiceInvocationHandler(Class<?> serviceInterface) {
            this.serviceInterface = serviceInterface;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 处理 Object 类的方法
            if (method.getDeclaringClass() == Object.class) {
                if ("toString".equals(method.getName())) {
                    return "NettyRpcProxy[" + serviceInterface.getName() + "]";
                } else if ("hashCode".equals(method.getName())) {
                    return serviceInterface.hashCode();
                } else if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                }
                throw new UnsupportedOperationException("不支持的方法: " + method.getName());
            }

            // 创建 RPC 请求
            String requestId = UUID.randomUUID().toString();
            RpcRequest request = new RpcRequest(
                    requestId,
                    serviceInterface.getName(),
                    method.getName(),
                    method.getParameterTypes(),
                    args);

            logger.info("发起RPC调用: {}", request);

            // 发送请求并获取响应
            CompletableFuture<RpcResponse> future = sendRequest(request);
            RpcResponse response = future.get(30, TimeUnit.SECONDS); // 30秒超时

            logger.info("收到RPC响应: {}", response);

            // 处理响应
            if (response.hasException()) {
                throw new RuntimeException("远程调用异常", response.getException());
            }

            return response.getResult();
        }
    }

    /**
     * RPC 客户端处理器
     */
    private static class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
        private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

        private final ConcurrentHashMap<String, CompletableFuture<RpcResponse>> pendingRequests;

        public RpcClientHandler(ConcurrentHashMap<String, CompletableFuture<RpcResponse>> pendingRequests) {
            this.pendingRequests = pendingRequests;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) {
            String requestId = response.getRequestId();
            CompletableFuture<RpcResponse> future = pendingRequests.remove(requestId);

            if (future != null) {
                future.complete(response);
            } else {
                logger.warn("收到未知请求ID的响应: {}", requestId);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("客户端处理异常", cause);
            ctx.close();
        }
    }
}
