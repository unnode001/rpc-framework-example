package com.example.rpc.example.nio;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.rpc.protocol.RpcRequest;
import com.example.rpc.protocol.RpcResponse;
import com.example.rpc.serialization.Serializer;
import com.example.rpc.serialization.SerializerFactory;

/**
 * 序列化性能测试
 */
public class SerializationBenchmark {
    private static final Logger logger = LoggerFactory.getLogger(SerializationBenchmark.class);

    public static void main(String[] args) {
        // 测试数据
        RpcRequest request = new RpcRequest(
                UUID.randomUUID().toString(),
                "com.example.rpc.example.HelloService",
                "sayHello",
                new Class[] { String.class },
                new Object[] { "Benchmark Test" });

        RpcResponse response = new RpcResponse(request.getRequestId(), "Hello, Benchmark Test!");

        // 测试所有序列化器
        String[] serializers = SerializerFactory.getSupportedSerializers();
        int testCount = 10000;

        logger.info("开始序列化性能测试，测试次数: {}", testCount);
        logger.info("测试对象: RpcRequest 和 RpcResponse");
        logger.info("================================");

        for (String serializerName : serializers) {
            try {
                testSerializer(serializerName, request, response, testCount);
            } catch (Exception e) {
                logger.error("测试序列化器 {} 失败", serializerName, e);
            }
        }
    }

    private static void testSerializer(String serializerName, RpcRequest request, RpcResponse response, int testCount) {
        Serializer serializer = SerializerFactory.getSerializer(serializerName);

        // 预热
        for (int i = 0; i < 1000; i++) {
            byte[] data = serializer.serialize(request);
            serializer.deserialize(data, RpcRequest.class);
        }

        // 测试请求序列化
        long startTime = System.nanoTime();
        byte[] requestData = null;
        for (int i = 0; i < testCount; i++) {
            requestData = serializer.serialize(request);
        }
        long requestSerializeTime = System.nanoTime() - startTime;

        // 测试请求反序列化
        startTime = System.nanoTime();
        for (int i = 0; i < testCount; i++) {
            serializer.deserialize(requestData, RpcRequest.class);
        }
        long requestDeserializeTime = System.nanoTime() - startTime;

        // 测试响应序列化
        startTime = System.nanoTime();
        byte[] responseData = null;
        for (int i = 0; i < testCount; i++) {
            responseData = serializer.serialize(response);
        }
        long responseSerializeTime = System.nanoTime() - startTime;

        // 测试响应反序列化
        startTime = System.nanoTime();
        for (int i = 0; i < testCount; i++) {
            serializer.deserialize(responseData, RpcResponse.class);
        }
        long responseDeserializeTime = System.nanoTime() - startTime;

        // 计算结果
        double requestSerializeAvg = requestSerializeTime / 1_000_000.0 / testCount;
        double requestDeserializeAvg = requestDeserializeTime / 1_000_000.0 / testCount;
        double responseSerializeAvg = responseSerializeTime / 1_000_000.0 / testCount;
        double responseDeserializeAvg = responseDeserializeTime / 1_000_000.0 / testCount;

        logger.info("序列化器: {}", serializerName.toUpperCase());
        logger.info("  请求数据大小: {} bytes", requestData.length);
        logger.info("  响应数据大小: {} bytes", responseData.length);
        logger.info("  请求序列化平均耗时: {} ms", String.format("%.3f", requestSerializeAvg));
        logger.info("  请求反序列化平均耗时: {} ms", String.format("%.3f", requestDeserializeAvg));
        logger.info("  响应序列化平均耗时: {} ms", String.format("%.3f", responseSerializeAvg));
        logger.info("  响应反序列化平均耗时: {} ms", String.format("%.3f", responseDeserializeAvg));
        logger.info("  总平均耗时: {} ms",
                String.format("%.3f",
                        requestSerializeAvg + requestDeserializeAvg + responseSerializeAvg + responseDeserializeAvg));
        logger.info("--------------------------------");
    }
}
