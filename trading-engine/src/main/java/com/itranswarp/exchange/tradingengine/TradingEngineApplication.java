package com.itranswarp.exchange.tradingengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.itranswarp.exchange.config.MessagingConfiguration;
import com.itranswarp.exchange.config.RedisConfiguration;

/**
 * 交易引擎服务入口。
 */
@SpringBootApplication
@EnableScheduling
@EntityScan("com.itranswarp.exchange.model")
@Import({ RedisConfiguration.class, MessagingConfiguration.class })
public class TradingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingEngineApplication.class, args);
    }
}
