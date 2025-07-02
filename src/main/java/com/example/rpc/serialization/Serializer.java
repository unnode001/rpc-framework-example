package com.example.rpc.serialization;

/**
 * 序列化接口
 * 提供统一的序列化和反序列化接口
 */
public interface Serializer {

    /**
     * 序列化对象
     * 
     * @param obj 要序列化的对象
     * @return 序列化后的字节数组
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化对象
     * 
     * @param bytes 字节数组
     * @param clazz 目标类型
     * @return 反序列化后的对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);

    /**
     * 获取序列化器名称
     * 
     * @return 序列化器名称
     */
    String getName();
}
