package com.example.rpc.protocol;

import java.io.Serializable;

/**
 * RPC 请求对象
 * 包含接口名称、方法名称、参数类型列表、参数值列表、请求ID
 */
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId; // 请求ID，用于请求响应匹配
    private String interfaceName; // 接口名称
    private String methodName; // 方法名称
    private Class<?>[] paramTypes; // 参数类型列表
    private Object[] params; // 参数值列表

    public RpcRequest() {
    }

    public RpcRequest(String requestId, String interfaceName, String methodName,
            Class<?>[] paramTypes, Object[] params) {
        this.requestId = requestId;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.paramTypes = paramTypes;
        this.params = params;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(Class<?>[] paramTypes) {
        this.paramTypes = paramTypes;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "RpcRequest{" +
                "requestId='" + requestId + '\'' +
                ", interfaceName='" + interfaceName + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
