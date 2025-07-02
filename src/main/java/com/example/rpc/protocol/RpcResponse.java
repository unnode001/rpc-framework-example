package com.example.rpc.protocol;

import java.io.Serializable;

/**
 * RPC 响应对象
 * 包含返回值、异常信息、与请求ID对应的ID
 */
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId; // 对应的请求ID
    private Object result; // 返回值
    private Throwable exception; // 异常信息

    public RpcResponse() {
    }

    public RpcResponse(String requestId, Object result) {
        this.requestId = requestId;
        this.result = result;
    }

    public RpcResponse(String requestId, Throwable exception) {
        this.requestId = requestId;
        this.exception = exception;
    }

    /**
     * 判断是否有异常
     */
    public boolean hasException() {
        return exception != null;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId='" + requestId + '\'' +
                ", result=" + result +
                ", exception=" + exception +
                '}';
    }
}
