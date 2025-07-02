package com.example.rpc.async;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.client.IRpcClient;
import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

/**
 * 异步 RPC 客户端代理
 */
public class AsyncRpcClientProxy implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(AsyncRpcClientProxy.class);

    private final IRpcClient rpcClient;
    private final Class<?> serviceInterface;

    public AsyncRpcClientProxy(IRpcClient rpcClient, Class<?> serviceInterface) {
        this.rpcClient = rpcClient;
        this.serviceInterface = serviceInterface;
    }

    /**
     * 创建异步代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> AsyncServiceProxy<T> create(IRpcClient rpcClient, Class<T> serviceInterface) {
        AsyncRpcClientProxy handler = new AsyncRpcClientProxy(rpcClient, serviceInterface);
        T proxy = (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[] { serviceInterface },
                handler);
        return new AsyncServiceProxy<>(proxy, handler);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 如果是异步调用（返回 CompletableFuture）
        if (CompletableFuture.class.isAssignableFrom(method.getReturnType())) {
            return invokeAsync(method, args);
        }

        // 同步调用
        return invokeSync(method, args);
    }

    /**
     * 异步调用
     */
    private CompletableFuture<Object> invokeAsync(Method method, Object[] args) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return invokeSync(method, args);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * 同步调用
     */
    private Object invokeSync(Method method, Object[] args) throws Exception {
        // 构建请求
        RpcRequest request = new RpcRequest();
        request.setRequestId(generateRequestId());
        request.setInterfaceName(serviceInterface.getName());
        request.setMethodName(method.getName());
        request.setParamTypes(method.getParameterTypes());
        request.setParams(args);

        logger.debug("发送异步 RPC 请求: {}", request);

        // 发送请求
        RpcResponse response = rpcClient.sendRequest(request);

        if (response.hasException()) {
            throw new RuntimeException(response.getException());
        }

        return response.getResult();
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return Thread.currentThread().getName() + "-" + System.currentTimeMillis();
    }

    /**
     * 异步服务代理包装类
     */
    public static class AsyncServiceProxy<T> {
        private final T proxy;
        private final AsyncRpcClientProxy handler;

        public AsyncServiceProxy(T proxy, AsyncRpcClientProxy handler) {
            this.proxy = proxy;
            this.handler = handler;
        }

        /**
         * 获取代理对象
         */
        public T getProxy() {
            return proxy;
        }

        /**
         * 异步调用方法
         */
        public CompletableFuture<Object> callAsync(String methodName, Object... args) {
            try {
                Method method = handler.serviceInterface.getMethod(methodName, getParameterTypes(args));
                return handler.invokeAsync(method, args);
            } catch (Exception e) {
                CompletableFuture<Object> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        }

        /**
         * 获取参数类型
         */
        private Class<?>[] getParameterTypes(Object[] args) {
            if (args == null || args.length == 0) {
                return new Class<?>[0];
            }

            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            return types;
        }
    }
}
