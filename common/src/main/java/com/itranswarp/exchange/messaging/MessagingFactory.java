package com.itranswarp.exchange.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itranswarp.exchange.message.AbstractMessage;
import com.itranswarp.exchange.support.LoggerSupport;

/**
 * 消息工厂：基于 Kafka 创建生产者与批量消费者。
 * <p>
 * 消息统一以 JSON 文本传输，通过 {@link AbstractMessage} 的多态类型信息在消费端还原具体类型，
 * 因此业务层无需关心序列化细节。
 * <p>
 * 通过 {@code MessagingConfiguration} 以 {@code @Bean} 方式注入，仅在需要消息能力的模块中启用。
 */
public class MessagingFactory extends LoggerSupport {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    public MessagingFactory(ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate,
            KafkaProperties kafkaProperties) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    /**
     * 创建指定主题的消息生产者。
     */
    public <T extends AbstractMessage> MessageProducer<T> createMessageProducer(Messaging.Topic topic) {
        logger.info("create message producer for topic {}...", topic);
        final String topicName = topic.name();
        return message -> {
            try {
                String data = objectMapper.writeValueAsString(message);
                kafkaTemplate.send(topicName, data);
            } catch (Exception e) {
                throw new RuntimeException("Failed to send message to topic " + topicName, e);
            }
        };
    }

    /**
     * 创建指定主题的批量消费者，收到一批消息后回调 handler。
     */
    public <T extends AbstractMessage> MessageConsumer createBatchMessageListener(Messaging.Topic topic, String groupId,
            BatchMessageHandler<T> handler) {
        logger.info("create batch message listener for topic {}, group {}...", topic, groupId);

        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaProperties.getBootstrapServers());
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.FALSE);
        configs.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(configs);

        ContainerProperties containerProperties = new ContainerProperties(topic.name());
        containerProperties.setAckMode(AckMode.BATCH);
        containerProperties.setMessageListener((BatchMessageListener<String, String>) records -> {
            List<T> messages = new ArrayList<>(records.size());
            for (ConsumerRecord<String, String> record : records) {
                messages.add(parseMessage(record.value()));
            }
            handler.processMessages(messages);
        });

        ConcurrentMessageListenerContainer<String, String> container = new ConcurrentMessageListenerContainer<>(
                consumerFactory, containerProperties);
        container.setConcurrency(1);
        container.start();
        return container::stop;
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractMessage> T parseMessage(String data) {
        try {
            return (T) objectMapper.readValue(data, AbstractMessage.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse message: " + data, e);
        }
    }
}
