package com.example.rpc.serialization.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.example.rpc.serialization.Serializer;

/**
 * Hessian 序列化实现
 */
public class HessianSerializer implements Serializer {

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            HessianOutput hessianOutput = new HessianOutput(baos);
            hessianOutput.writeObject(obj);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Hessian序列化失败", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            HessianInput hessianInput = new HessianInput(bais);
            return (T) hessianInput.readObject();
        } catch (IOException e) {
            throw new RuntimeException("Hessian反序列化失败", e);
        }
    }

    @Override
    public String getName() {
        return "hessian";
    }
}
