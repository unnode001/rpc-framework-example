package com.example.rpc.registry;

import java.util.List;

/**
 * 服务注册与发现接口
 */
public interface ServiceRegistry {

    /**
     * 注册服务
     * 
     * @param serviceName    服务名称
     * @param serviceAddress 服务地址
     */
    void registerService(String serviceName, String serviceAddress);

    /**
     * 发现服务
     * 
     * @param serviceName 服务名称
     * @return 服务地址列表
     */
    List<String> discoverService(String serviceName);

    /**
     * 注销服务
     * 
     * @param serviceName    服务名称
     * @param serviceAddress 服务地址
     */
    void unregisterService(String serviceName, String serviceAddress);

    /**
     * 获取所有已注册的服务名称
     * 
     * @return 服务名称列表
     */
    List<String> getAllServices();

    /**
     * 获取指定服务的所有实例
     * 
     * @param serviceName 服务名称
     * @return 服务实例地址列表
     */
    List<String> getServiceInstances(String serviceName);

    /**
     * 关闭注册中心连接
     */
    void close();
}
