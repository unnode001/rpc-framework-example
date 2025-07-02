package com.example.rpc.loadbalancer;

import java.util.List;

/**
 * 负载均衡接口
 */
public interface LoadBalancer {

    /**
     * 从服务列表中选择一个服务
     * 
     * @param services  服务地址列表
     * @param requestId 请求ID，用于一致性哈希等算法
     * @return 选中的服务地址
     */
    String select(List<String> services, String requestId);

    /**
     * 获取负载均衡算法名称
     * 
     * @return 算法名称
     */
    String getName();
}
