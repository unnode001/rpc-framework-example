package com.example.rpc.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC 框架配置管理
 */
public class RpcConfig {
    private static final Logger logger = LoggerFactory.getLogger(RpcConfig.class);

    private static final String DEFAULT_CONFIG_FILE = "rpc.properties";
    private final Properties properties;

    // 单例模式
    private static volatile RpcConfig instance;

    private RpcConfig() {
        this.properties = new Properties();
        loadConfig();
    }

    public static RpcConfig getInstance() {
        if (instance == null) {
            synchronized (RpcConfig.class) {
                if (instance == null) {
                    instance = new RpcConfig();
                }
            }
        }
        return instance;
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
                logger.info("加载配置文件成功: {}", DEFAULT_CONFIG_FILE);
            } else {
                logger.warn("配置文件不存在，使用默认配置: {}", DEFAULT_CONFIG_FILE);
                setDefaultConfig();
            }
        } catch (IOException e) {
            logger.error("加载配置文件失败，使用默认配置", e);
            setDefaultConfig();
        }
    }

    /**
     * 设置默认配置
     */
    private void setDefaultConfig() {
        // 服务器配置
        properties.setProperty("rpc.server.port", "8080");
        properties.setProperty("rpc.server.threads", "200");

        // 客户端配置
        properties.setProperty("rpc.client.timeout", "5000");
        properties.setProperty("rpc.client.retries", "3");
        properties.setProperty("rpc.client.retry.delay", "1000");

        // 序列化配置
        properties.setProperty("rpc.serialization.type", "kryo");

        // 注册中心配置
        properties.setProperty("rpc.registry.type", "nacos");
        properties.setProperty("rpc.registry.address", "127.0.0.1:8848");
        properties.setProperty("rpc.registry.namespace", "public");

        // 负载均衡配置
        properties.setProperty("rpc.loadbalancer.type", "random");

        // 容错配置
        properties.setProperty("rpc.fault.tolerance", "fail_fast");

        // 健康检查配置
        properties.setProperty("rpc.health.check.enabled", "true");
        properties.setProperty("rpc.health.check.interval", "30");
    }

    // 获取配置方法

    public int getServerPort() {
        return getInt("rpc.server.port", 8080);
    }

    public int getServerThreads() {
        return getInt("rpc.server.threads", 200);
    }

    public int getClientTimeout() {
        return getInt("rpc.client.timeout", 5000);
    }

    public int getClientRetries() {
        return getInt("rpc.client.retries", 3);
    }

    public long getClientRetryDelay() {
        return getLong("rpc.client.retry.delay", 1000L);
    }

    public String getSerializationType() {
        return getString("rpc.serialization.type", "kryo");
    }

    public String getRegistryType() {
        return getString("rpc.registry.type", "nacos");
    }

    public String getRegistryAddress() {
        return getString("rpc.registry.address", "127.0.0.1:8848");
    }

    public String getRegistryNamespace() {
        return getString("rpc.registry.namespace", "public");
    }

    public String getLoadBalancerType() {
        return getString("rpc.loadbalancer.type", "random");
    }

    public String getFaultToleranceStrategy() {
        return getString("rpc.fault.tolerance", "fail_fast");
    }

    public boolean isHealthCheckEnabled() {
        return getBoolean("rpc.health.check.enabled", true);
    }

    public int getHealthCheckInterval() {
        return getInt("rpc.health.check.interval", 30);
    }

    // 通用获取方法

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warn("配置项 {} 的值 {} 不是有效整数，使用默认值 {}", key, properties.getProperty(key), defaultValue);
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warn("配置项 {} 的值 {} 不是有效长整数，使用默认值 {}", key, properties.getProperty(key), defaultValue);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * 设置配置项
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * 获取所有配置
     */
    public Properties getAllProperties() {
        return new Properties(properties);
    }
}
