package com.example.rpc.loadbalancer;

import java.util.HashMap;
import java.util.Map;

import com.example.rpc.loadbalancer.impl.ConsistentHashLoadBalancer;
import com.example.rpc.loadbalancer.impl.RandomLoadBalancer;
import com.example.rpc.loadbalancer.impl.RoundRobinLoadBalancer;

/**
 * 负载均衡器工厂
 */
public class LoadBalancerFactory {

    private static final Map<String, LoadBalancer> loadBalancers = new HashMap<>();

    static {
        loadBalancers.put("random", new RandomLoadBalancer());
        loadBalancers.put("roundrobin", new RoundRobinLoadBalancer());
        loadBalancers.put("consistenthash", new ConsistentHashLoadBalancer());
    }

    /**
     * 根据名称获取负载均衡器
     * 
     * @param name 负载均衡器名称
     * @return 负载均衡器实例
     */
    public static LoadBalancer getLoadBalancer(String name) {
        LoadBalancer loadBalancer = loadBalancers.get(name);
        if (loadBalancer == null) {
            throw new IllegalArgumentException("不支持的负载均衡器: " + name);
        }
        return loadBalancer;
    }

    /**
     * 获取默认负载均衡器
     * 
     * @return 默认负载均衡器
     */
    public static LoadBalancer getDefaultLoadBalancer() {
        return getLoadBalancer("roundrobin"); // 默认使用轮询
    }

    /**
     * 创建负载均衡器（别名方法）
     * 
     * @param name 负载均衡器名称
     * @return 负载均衡器实例
     */
    public static LoadBalancer createLoadBalancer(String name) {
        return getLoadBalancer(name);
    }

    /**
     * 获取所有支持的负载均衡器名称
     * 
     * @return 负载均衡器名称数组
     */
    public static String[] getSupportedLoadBalancers() {
        return loadBalancers.keySet().toArray(new String[0]);
    }
}
