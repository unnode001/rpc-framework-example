package com.example.rpc.loadbalancer.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.rpc.loadbalancer.LoadBalancer;

/**
 * 轮询负载均衡实现
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String select(List<String> services, String requestId) {
        if (services == null || services.isEmpty()) {
            return null;
        }

        if (services.size() == 1) {
            return services.get(0);
        }

        int index = counter.getAndIncrement() % services.size();
        return services.get(index);
    }

    @Override
    public String getName() {
        return "roundrobin";
    }
}
