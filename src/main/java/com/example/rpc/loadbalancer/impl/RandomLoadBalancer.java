package com.example.rpc.loadbalancer.impl;

import java.util.List;
import java.util.Random;

import com.example.rpc.loadbalancer.LoadBalancer;

/**
 * 随机负载均衡实现
 */
public class RandomLoadBalancer implements LoadBalancer {

    private final Random random = new Random();

    @Override
    public String select(List<String> services, String requestId) {
        if (services == null || services.isEmpty()) {
            return null;
        }

        if (services.size() == 1) {
            return services.get(0);
        }

        int index = random.nextInt(services.size());
        return services.get(index);
    }

    @Override
    public String getName() {
        return "random";
    }
}
