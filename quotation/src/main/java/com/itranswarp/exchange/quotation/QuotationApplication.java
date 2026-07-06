package com.itranswarp.exchange.quotation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;

import com.itranswarp.exchange.config.MessagingConfiguration;
import com.itranswarp.exchange.config.RedisConfiguration;

/**
 * 行情服务入口。
 * <p>
 * 消费 TICK 主题中的成交 Tick，聚合为秒/分/时/日 4 种粒度的 K 线并持久化。
 */
@SpringBootApplication
// 扫描 common 模块中的实体 (Tick 与各粒度 Bar):
@EntityScan("com.itranswarp.exchange.model")
// 启用 Redis 与消息 (Kafka) 相关 Bean:
@Import({ RedisConfiguration.class, MessagingConfiguration.class })
public class QuotationApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuotationApplication.class, args);
    }
}
