package com.example.rpc.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

/**
 * RPC 客户端
 * 使用动态代理创建服务接口的代理对象
 */
public class RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private final String host;
    private final int port;

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
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
                    return "RpcProxy[" + serviceInterface.getName() + "]";
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
            RpcResponse response = sendRequest(request);

            logger.info("收到RPC响应: {}", response);

            // 处理响应
            if (response.hasException()) {
                throw new RuntimeException("远程调用异常", response.getException());
            }

            return response.getResult();
        }
    }

    /**
     * 发送RPC请求
     */
    private RpcResponse sendRequest(RpcRequest request) throws Exception {
        try (Socket socket = new Socket(host, port);
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {

            // 发送请求
            output.writeObject(request);
            output.flush();

            // 接收响应
            return (RpcResponse) input.readObject();
        }
    }
}
