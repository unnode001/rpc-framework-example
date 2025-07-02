package com.example.rpc.serialization.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.example.rpc.serialization.Serializer;

/**
 * Java 原生序列化实现
 */
public class JavaSerializer implements Serializer {

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Java序列化失败", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Java反序列化失败", e);
        }
    }

    @Override
    public String getName() {
        return "java";
    }
}
