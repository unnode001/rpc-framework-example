package com.example.rpc.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

/**
 * RPC 服务器
 * 使用 BIO + 线程池模型处理客户端请求
 */
public class RpcServer {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private final int port;
    private final ConcurrentHashMap<String, Object> serviceMap = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private boolean isRunning = false;

    public RpcServer(int port) {
        this.port = port;
    }

    /**
     * 注册服务实现
     */
    public void registerService(Class<?> serviceInterface, Object serviceImpl) {
        String serviceName = serviceInterface.getName();
        serviceMap.put(serviceName, serviceImpl);
        logger.info("注册服务: {}", serviceName);
    }

    /**
     * 启动服务器
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            isRunning = true;
            logger.info("RPC 服务器启动，监听端口: {}", port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                logger.info("接收到客户端连接: {}", clientSocket.getRemoteSocketAddress());

                // 为每个客户端连接创建一个处理任务
                executor.submit(new RequestHandler(clientSocket));
            }
        } catch (IOException e) {
            logger.error("服务器启动失败", e);
        }
    }

    /**
     * 停止服务器
     */
    public void stop() {
        isRunning = false;
        executor.shutdown();
        logger.info("RPC 服务器已停止");
    }

    /**
     * 请求处理器
     */
    private class RequestHandler implements Runnable {
        private final Socket socket;

        public RequestHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {

                // 读取请求
                RpcRequest request = (RpcRequest) input.readObject();
                logger.info("接收到请求: {}", request);

                // 处理请求
                RpcResponse response = handleRequest(request);
                logger.info("处理完成，返回响应: {}", response);

                // 发送响应
                output.writeObject(response);
                output.flush();

            } catch (Exception e) {
                logger.error("处理请求时发生异常", e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.error("关闭连接时发生异常", e);
                }
            }
        }
    }

    /**
     * 处理RPC请求
     */
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
