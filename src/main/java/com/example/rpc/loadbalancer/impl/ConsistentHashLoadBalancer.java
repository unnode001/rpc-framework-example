package com.example.rpc.loadbalancer.impl;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.example.rpc.loadbalancer.LoadBalancer;

/**
 * 一致性哈希负载均衡实现
 */
public class ConsistentHashLoadBalancer implements LoadBalancer {

    private static final int VIRTUAL_NODES = 160; // 虚拟节点数

    @Override
    public String select(List<String> services, String requestId) {
        if (services == null || services.isEmpty()) {
            return null;
        }

        if (services.size() == 1) {
            return services.get(0);
        }

        // 构建一致性哈希环
        TreeMap<Integer, String> ring = new TreeMap<>();
        for (String service : services) {
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                int hash = hash(service + "#" + i);
                ring.put(hash, service);
            }
        }

        // 计算请求的哈希值
        int requestHash = hash(requestId);

        // 在环上找到第一个大于等于该哈希值的节点
        SortedMap<Integer, String> tailMap = ring.tailMap(requestHash);
        Integer key = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();

        return ring.get(key);
    }

    @Override
    public String getName() {
        return "consistenthash";
    }

    /**
     * 简单的哈希函数
     */
    private int hash(String key) {
        return Math.abs(key.hashCode());
    }
}
