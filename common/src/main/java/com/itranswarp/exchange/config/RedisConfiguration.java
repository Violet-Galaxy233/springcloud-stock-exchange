package com.itranswarp.exchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.itranswarp.exchange.redis.RedisService;

/**
 * Redis 配置。需要 Redis 能力的模块通过 {@code @Import(RedisConfiguration.class)} 启用。
 * <p>
 * {@link RedisConnectionFactory} 由 Spring Boot 根据 {@code spring.data.redis.*} 自动配置。
 */
@Configuration
public class RedisConfiguration {

    @Bean
    public RedisService redisService(RedisConnectionFactory redisConnectionFactory) {
        return new RedisService(redisConnectionFactory);
    }
}
