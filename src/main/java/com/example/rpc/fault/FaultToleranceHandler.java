package com.example.rpc.fault;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;

/**
 * 容错处理器
 */
public class FaultToleranceHandler {
    private static final Logger logger = LoggerFactory.getLogger(FaultToleranceHandler.class);

    private final FaultToleranceStrategy strategy;
    private final int maxRetries;
    private final long retryDelay;

    public FaultToleranceHandler(FaultToleranceStrategy strategy) {
        this(strategy, 3, 1000);
    }

    public FaultToleranceHandler(FaultToleranceStrategy strategy, int maxRetries, long retryDelay) {
        this.strategy = strategy;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }

    /**
     * 处理调用失败
     */
    public RpcResponse handleFailure(RpcRequest request, List<String> availableInstances,
            CallExecutor executor, Exception lastException) {
        switch (strategy) {
            case FAIL_FAST:
                return handleFailFast(lastException);
            case FAIL_RETRY:
                return handleFailRetry(request, executor, lastException);
            case FAIL_OVER:
                return handleFailOver(request, availableInstances, executor, lastException);
            case FAIL_SAFE:
                return handleFailSafe(request, lastException);
            case FAIL_BACK:
                return handleFailBack(request, lastException);
            default:
                return handleFailFast(lastException);
        }
    }

    /**
     * 快速失败处理
     */
    private RpcResponse handleFailFast(Exception exception) {
        logger.error("快速失败: {}", exception.getMessage());
        return new RpcResponse(null, exception);
    }

    /**
     * 重试处理
     */
    private RpcResponse handleFailRetry(RpcRequest request, CallExecutor executor, Exception lastException) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(retryDelay * (i + 1)); // 指数退避
                logger.info("第 {} 次重试调用: {}", i + 1, request);
                return executor.execute(request);
            } catch (Exception e) {
                lastException = e;
                logger.warn("第 {} 次重试失败: {}", i + 1, e.getMessage());
            }
        }

        logger.error("重试 {} 次后仍然失败", maxRetries);
        return new RpcResponse(request.getRequestId(), lastException);
    }

    /**
     * 失败转移处理
     */
    private RpcResponse handleFailOver(RpcRequest request, List<String> availableInstances,
            CallExecutor executor, Exception lastException) {
        if (availableInstances == null || availableInstances.isEmpty()) {
            logger.error("没有可用的服务实例进行失败转移");
            return new RpcResponse(request.getRequestId(), lastException);
        }

        // 尝试其他实例
        for (String instance : availableInstances) {
            try {
                logger.info("尝试转移到实例: {}", instance);
                return executor.executeOnInstance(request, instance);
            } catch (Exception e) {
                logger.warn("实例 {} 调用失败: {}", instance, e.getMessage());
                lastException = e;
            }
        }

        logger.error("所有实例调用失败");
        return new RpcResponse(request.getRequestId(), lastException);
    }

    /**
     * 失败安全处理
     */
    private RpcResponse handleFailSafe(RpcRequest request, Exception exception) {
        logger.warn("失败安全处理，返回空结果: {}", exception.getMessage());
        return new RpcResponse(request.getRequestId(), null);
    }

    /**
     * 失败回退处理
     */
    private RpcResponse handleFailBack(RpcRequest request, Exception exception) {
        logger.warn("失败回退处理: {}", exception.getMessage());
        // 这里可以返回缓存的结果或默认值
        return new RpcResponse(request.getRequestId(), getDefaultResult(request));
    }

    /**
     * 获取默认结果
     */
    private Object getDefaultResult(RpcRequest request) {
        // 根据方法返回类型返回默认值
        // 这里简化处理，实际应该根据方法签名来判断
        return null;
    }

    /**
     * 调用执行器接口
     */
    public interface CallExecutor {
        RpcResponse execute(RpcRequest request) throws Exception;

        RpcResponse executeOnInstance(RpcRequest request, String instance) throws Exception;
    }
}
