package com.example.rpc.example;

/**
 * 示例服务接口
 */
public interface HelloService {

    /**
     * 简单的问候方法
     */
    String sayHello(String name);

    /**
     * 计算两个数的和
     */
    int add(int a, int b);

    /**
     * 获取当前时间戳
     */
    long getCurrentTime();
}
