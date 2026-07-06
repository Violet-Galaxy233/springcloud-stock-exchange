package com.itranswarp.exchange.push;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.itranswarp.exchange.config.RedisConfiguration;

/**
 * 推送服务入口。订阅 Redis 通知频道，通过 WebSocket 转发给浏览器客户端。
 */
@SpringBootApplication
@Import(RedisConfiguration.class)
public class PushApplication {

    public static void main(String[] args) {
        SpringApplication.run(PushApplication.class, args);
    }
}
