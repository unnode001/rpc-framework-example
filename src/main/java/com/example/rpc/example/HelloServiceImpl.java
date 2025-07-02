package com.example.rpc.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HelloService 的实现
 */
public class HelloServiceImpl implements HelloService {
    private static final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public String sayHello(String name) {
        String message = "Hello, " + name + "!";
        logger.info("调用 sayHello 方法，参数: {}, 返回: {}", name, message);
        return message;
    }

    @Override
    public int add(int a, int b) {
        int result = a + b;
        logger.info("调用 add 方法，参数: {}, {}, 返回: {}", a, b, result);
        return result;
    }

    @Override
    public long getCurrentTime() {
        long currentTime = System.currentTimeMillis();
        logger.info("调用 getCurrentTime 方法，返回: {}", currentTime);
        return currentTime;
    }
}
