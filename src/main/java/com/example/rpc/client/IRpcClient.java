package com.example.rpc.client;

import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

/**
 * RPC 客户端接口
 */
public interface IRpcClient {

    /**
     * 发送 RPC 请求
     * 
     * @param request RPC 请求
     * @return RPC 响应
     * @throws Exception 请求异常
     */
    RpcResponse sendRequest(RpcRequest request) throws Exception;

    /**
     * 创建服务代理
     * 
     * @param <T>              服务接口类型
     * @param serviceInterface 服务接口
     * @return 服务代理对象
     */
    <T> T createProxy(Class<T> serviceInterface);

    /**
     * 关闭客户端
     */
    void close();
}
