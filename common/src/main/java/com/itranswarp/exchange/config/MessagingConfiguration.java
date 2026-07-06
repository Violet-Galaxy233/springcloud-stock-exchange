package com.itranswarp.exchange.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itranswarp.exchange.messaging.MessagingFactory;

/**
 * 消息 (Kafka) 配置。需要消息能力的模块通过 {@code @Import(MessagingConfiguration.class)} 启用。
 * <p>
 * {@link KafkaTemplate} 与 {@link KafkaProperties} 由 Spring Boot 根据 {@code spring.kafka.*} 自动配置。
 */
@Configuration
public class MessagingConfiguration {

    @Bean
    public MessagingFactory messagingFactory(ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate,
            KafkaProperties kafkaProperties) {
        return new MessagingFactory(objectMapper, kafkaTemplate, kafkaProperties);
    }
}
