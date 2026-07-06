package com.itranswarp.exchange.tradingengine;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.message.event.AbstractEvent;
import com.itranswarp.exchange.message.event.OrderRequestEvent;
import com.itranswarp.exchange.message.event.TransferEvent;
import com.itranswarp.exchange.messaging.MessageConsumer;
import com.itranswarp.exchange.messaging.MessageProducer;
import com.itranswarp.exchange.messaging.Messaging;
import com.itranswarp.exchange.messaging.MessagingFactory;
import com.itranswarp.exchange.tradingengine.assets.Asset;
import com.itranswarp.exchange.tradingengine.assets.AssetService;
import com.itranswarp.exchange.tradingengine.clearing.ClearingService;
import com.itranswarp.exchange.tradingengine.match.MatchEngine;
import com.itranswarp.exchange.tradingengine.order.OrderService;
import com.itranswarp.exchange.util.JsonUtil;

/**
 * 消息链路集成测试：使用内嵌 Kafka (纯 JVM，无需 Docker) 验证完整闭环 ——
 * 事件序列化为 JSON → 发送到 Kafka TRADE 主题 → 消费 → 依据 @class 多态反序列化回具体子类型
 * → 交易引擎处理，最终资产变化正确且系统资产守恒。
 * <p>
 * 这补足了纯内存单元测试未覆盖的部分：MessagingFactory 的生产者/消费者与 Jackson 多态序列化。
 */
class MessagingRoundTripIntegrationTest {

    static final Long DEBT = 1L;
    static final Long USER_A = 100L;
    static final Long USER_B = 200L;

    static EmbeddedKafkaKraftBroker broker;
    static MessagingFactory messagingFactory;

    final AtomicLong seq = new AtomicLong(0);

    @BeforeAll
    static void startKafka() {
        broker = new EmbeddedKafkaKraftBroker(1, 1, Messaging.Topic.TRADE.name());
        broker.afterPropertiesSet();
        String bootstrap = broker.getBrokersAsString();

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        KafkaTemplate<String, String> template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.setBootstrapServers(List.of(bootstrap));

        messagingFactory = new MessagingFactory(JsonUtil.getObjectMapper(), template, kafkaProperties);
    }

    @AfterAll
    static void stopKafka() {
        if (broker != null) {
            broker.destroy();
        }
    }

    @Test
    void eventsFlowThroughKafkaAndAreMatchedByEngine() throws InterruptedException {
        // 组装一个全内存交易引擎:
        AssetService assetService = new AssetService();
        OrderService orderService = new OrderService(assetService);
        MatchEngine matchEngine = new MatchEngine();
        ClearingService clearingService = new ClearingService(assetService, orderService);
        TradingEngineService engine = new TradingEngineService(assetService, orderService, matchEngine,
                clearingService);

        final int expectedEvents = 4;
        CountDownLatch latch = new CountDownLatch(expectedEvents);

        // 消费者：从 Kafka 收到消息后交给引擎处理:
        MessageConsumer consumer = messagingFactory.createBatchMessageListener(Messaging.Topic.TRADE, "test-engine",
                (List<AbstractEvent> messages) -> {
                    for (AbstractEvent event : messages) {
                        engine.processEvent(event);
                        latch.countDown();
                    }
                });

        try {
            // 等待消费者完成分区分配 (auto-offset-reset=latest，须先就位再生产):
            TimeUnit.SECONDS.sleep(8);

            MessageProducer<AbstractEvent> producer = messagingFactory
                    .createMessageProducer(Messaging.Topic.TRADE);
            // 充值 + 撮合:
            producer.sendMessage(deposit(USER_A, AssetEnum.USD, "100000"));
            producer.sendMessage(deposit(USER_B, AssetEnum.BTC, "100"));
            producer.sendMessage(order(USER_A, Direction.BUY, "20000", "1"));
            producer.sendMessage(order(USER_B, Direction.SELL, "20000", "1"));

            // 等待全部事件被处理:
            assertTrue(latch.await(30, TimeUnit.SECONDS),
                    "engine did not process all events in time (remaining=" + latch.getCount() + ")");
        } finally {
            consumer.stop();
        }

        // 校验撮合结果 (与纯内存测试一致，但这次经过了真实的 Kafka 序列化往返):
        assertEquals(new BigDecimal("80000"), assetService.getAsset(USER_A, AssetEnum.USD).getAvailable());
        assertEquals(new BigDecimal("1"), assetService.getAsset(USER_A, AssetEnum.BTC).getAvailable());
        assertEquals(new BigDecimal("20000"), assetService.getAsset(USER_B, AssetEnum.USD).getAvailable());
        assertEquals(new BigDecimal("99"), assetService.getAsset(USER_B, AssetEnum.BTC).getAvailable());
        assertEquals(new BigDecimal("20000"), engine.getMarketPrice());

        // 系统资产守恒:
        for (AssetEnum asset : AssetEnum.values()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (var userAssets : assetService.getUserAssets().values()) {
                Asset a = userAssets.get(asset);
                if (a != null) {
                    sum = sum.add(a.getAvailable()).add(a.getFrozen());
                }
            }
            assertEquals(0, sum.signum(), "Asset " + asset + " total must be zero but was " + sum);
        }
    }

    TransferEvent deposit(Long userId, AssetEnum asset, String amount) {
        TransferEvent e = new TransferEvent();
        stamp(e);
        e.fromUserId = DEBT;
        e.toUserId = userId;
        e.asset = asset;
        e.amount = new BigDecimal(amount);
        e.sufficient = false;
        return e;
    }

    OrderRequestEvent order(Long userId, Direction direction, String price, String quantity) {
        OrderRequestEvent e = new OrderRequestEvent();
        stamp(e);
        e.userId = userId;
        e.direction = direction;
        e.price = new BigDecimal(price);
        e.quantity = new BigDecimal(quantity);
        return e;
    }

    void stamp(AbstractEvent e) {
        long prev = this.seq.get();
        long id = this.seq.incrementAndGet();
        e.previousId = prev;
        e.sequenceId = id;
        e.createdAt = 1_000_000L + id;
    }
}
