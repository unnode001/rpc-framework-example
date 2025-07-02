package com.example.rpc.registry.impl;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.example.rpc.registry.ServiceRegistry;

/**
 * 基于 Nacos 的服务注册与发现实现
 */
public class NacosServiceRegistry implements ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(NacosServiceRegistry.class);

    private final NamingService namingService;

    public NacosServiceRegistry(String serverAddr) {
        try {
            this.namingService = NamingFactory.createNamingService(serverAddr);
            logger.info("连接到 Nacos 注册中心: {}", serverAddr);
        } catch (NacosException e) {
            logger.error("连接 Nacos 失败", e);
            throw new RuntimeException("连接 Nacos 失败", e);
        }
    }

    @Override
    public void registerService(String serviceName, String serviceAddress) {
        try {
            String[] parts = serviceAddress.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            Instance instance = new Instance();
            instance.setIp(host);
            instance.setPort(port);
            instance.setHealthy(true);
            instance.setEnabled(true);

            namingService.registerInstance(serviceName, instance);
            logger.info("注册服务到 Nacos: {} -> {}", serviceName, serviceAddress);

        } catch (Exception e) {
            logger.error("注册服务失败: {} -> {}", serviceName, serviceAddress, e);
            throw new RuntimeException("注册服务失败", e);
        }
    }

    @Override
    public List<String> discoverService(String serviceName) {
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName);
            List<String> addresses = instances.stream()
                    .filter(Instance::isHealthy)
                    .filter(Instance::isEnabled)
                    .map(instance -> instance.getIp() + ":" + instance.getPort())
                    .collect(Collectors.toList());

            logger.info("发现服务 {}: {}", serviceName, addresses);
            return addresses;

        } catch (NacosException e) {
            logger.error("发现服务失败: {}", serviceName, e);
            throw new RuntimeException("发现服务失败", e);
        }
    }

    @Override
    public void unregisterService(String serviceName, String serviceAddress) {
        try {
            String[] parts = serviceAddress.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            namingService.deregisterInstance(serviceName, host, port);
            logger.info("注销服务: {} -> {}", serviceName, serviceAddress);

        } catch (Exception e) {
            logger.error("注销服务失败: {} -> {}", serviceName, serviceAddress, e);
        }
    }

    @Override
    public void close() {
        try {
            if (namingService != null) {
                namingService.shutDown();
                logger.info("关闭 Nacos 连接");
            }
        } catch (Exception e) {
            logger.error("关闭 Nacos 连接失败", e);
        }
    }

    @Override
    public List<String> getAllServices() {
        try {
            ListView<String> servicesOfServer = namingService.getServicesOfServer(1, Integer.MAX_VALUE);
            return servicesOfServer.getData();
        } catch (Exception e) {
            logger.error("获取所有服务失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getServiceInstances(String serviceName) {
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName);
            return instances.stream()
                    .filter(Instance::isEnabled)
                    .filter(Instance::isHealthy)
                    .map(instance -> instance.getIp() + ":" + instance.getPort())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取服务实例失败: {}", serviceName, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取本机IP地址
     */
    public static String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.warn("获取本机IP失败，使用默认值 localhost", e);
            return "localhost";
        }
    }
}
