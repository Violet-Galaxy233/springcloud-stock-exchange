package com.itranswarp.exchange.tradingsequencer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;

import com.itranswarp.exchange.config.MessagingConfiguration;

/**
 * 定序服务入口。
 */
@SpringBootApplication
@EntityScan("com.itranswarp.exchange.model")
@Import(MessagingConfiguration.class)
public class TradingSequencerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingSequencerApplication.class, args);
    }
}
