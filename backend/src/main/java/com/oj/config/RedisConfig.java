package com.oj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    // 手动创建 StringRedisTemplate Bean
    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        // 创建连接工厂
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();

        // 创建 StringRedisTemplate
        return new StringRedisTemplate(factory);
    }
}