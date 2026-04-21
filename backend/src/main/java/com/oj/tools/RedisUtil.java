package com.oj.tools;

import jakarta.annotation.PostConstruct;  // 添加这个导入
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 静态实例，供静态方法使用
    private static RedisUtil instance;  // 改名避免混淆

    @PostConstruct  // 添加这个方法！Spring 会在依赖注入完成后自动调用
    public void init() {
        instance = this;
        System.out.println("RedisUtil 初始化成功");  // 可选，用于确认
    }

    private static RedisUtil getInstance() {
        if (instance == null) {
            throw new RuntimeException("RedisUtil 未初始化，请确保 Spring 容器已启动");
        }
        return instance;
    }

    /**
     * 插入键值对（永久有效）
     */
    public static void set(String key, String value) {
        getInstance().stringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 插入键值对并设置过期时间
     */
    public static void set(String key, String value, long timeout, TimeUnit unit) {
        getInstance().stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 插入键值对（秒为单位过期时间）
     */
    public static void setWithExpire(String key, String value, long seconds) {
        getInstance().stringRedisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    /**
     * 根据键查找值
     */
    public static String get(String key) {
        return getInstance().stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 根据键删除
     */
    public static boolean delete(String key) {
        return Boolean.TRUE.equals(getInstance().stringRedisTemplate.delete(key));
    }

    /**
     * 批量删除多个键
     */
    public static Long delete(String... keys) {
        return getInstance().stringRedisTemplate.delete(java.util.Arrays.asList(keys));
    }

    /**
     * 判断键是否存在
     */
    public static boolean exists(String key) {
        return Boolean.TRUE.equals(getInstance().stringRedisTemplate.hasKey(key));
    }

    /**
     * 设置过期时间
     */
    public static boolean expire(String key, long seconds) {
        return Boolean.TRUE.equals(getInstance().stringRedisTemplate.expire(key, seconds, TimeUnit.SECONDS));
    }

    /**
     * 获取键的剩余过期时间
     */
    public static Long getExpire(String key) {
        return getInstance().stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 如果键不存在则设置值
     */
    public static boolean setIfAbsent(String key, String value) {
        return Boolean.TRUE.equals(getInstance().stringRedisTemplate.opsForValue().setIfAbsent(key, value));
    }
}