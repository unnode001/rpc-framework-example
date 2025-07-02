package com.example.rpc.serialization;

import java.util.HashMap;
import java.util.Map;

import com.example.rpc.serialization.impl.HessianSerializer;
import com.example.rpc.serialization.impl.JavaSerializer;
import com.example.rpc.serialization.impl.KryoSerializer;

/**
 * 序列化器工厂
 * 提供不同序列化实现的获取
 */
public class SerializerFactory {

    private static final Map<String, Serializer> serializers = new HashMap<>();

    static {
        serializers.put("java", new JavaSerializer());
        serializers.put("kryo", new KryoSerializer());
        serializers.put("hessian", new HessianSerializer());
    }

    /**
     * 根据名称获取序列化器
     * 
     * @param name 序列化器名称
     * @return 序列化器实例
     */
    public static Serializer getSerializer(String name) {
        Serializer serializer = serializers.get(name);
        if (serializer == null) {
            throw new IllegalArgumentException("不支持的序列化器: " + name);
        }
        return serializer;
    }

    /**
     * 获取默认序列化器
     * 
     * @return 默认序列化器
     */
    public static Serializer getDefaultSerializer() {
        return getSerializer("kryo"); // 默认使用Kryo，性能较好
    }

    /**
     * 创建序列化器（别名方法）
     * 
     * @param name 序列化器名称
     * @return 序列化器实例
     */
    public static Serializer createSerializer(String name) {
        return getSerializer(name);
    }

    /**
     * 获取所有支持的序列化器名称
     * 
     * @return 序列化器名称数组
     */
    public static String[] getSupportedSerializers() {
        return serializers.keySet().toArray(new String[0]);
    }
}
