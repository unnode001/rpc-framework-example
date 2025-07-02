package com.example.rpc.serialization.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.example.rpc.serialization.Serializer;

/**
 * Kryo 序列化实现
 */
public class KryoSerializer implements Serializer {

    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        // 设置为true可以处理循环引用，但会降低性能
        kryo.setReferences(false);
        // 设置为true可以处理未注册的类，但会降低性能
        kryo.setRegistrationRequired(false);
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Output output = new Output(baos)) {
            Kryo kryo = kryoThreadLocal.get();
            kryo.writeObject(output, obj);
            output.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Kryo序列化失败", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                Input input = new Input(bais)) {
            Kryo kryo = kryoThreadLocal.get();
            return kryo.readObject(input, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Kryo反序列化失败", e);
        }
    }

    @Override
    public String getName() {
        return "kryo";
    }
}
