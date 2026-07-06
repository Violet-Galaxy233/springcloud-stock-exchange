package com.itranswarp.exchange.tradingapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;

import com.itranswarp.exchange.config.MessagingConfiguration;
import com.itranswarp.exchange.config.RedisConfiguration;

/**
 * 交易 API 服务入口。
 */
@SpringBootApplication
@EntityScan("com.itranswarp.exchange.model")
@Import({ RedisConfiguration.class, MessagingConfiguration.class })
public class TradingApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingApiApplication.class, args);
    }
}
