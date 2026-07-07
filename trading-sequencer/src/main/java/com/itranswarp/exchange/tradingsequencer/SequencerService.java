package com.itranswarp.exchange.tradingsequencer;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.exchange.message.event.AbstractEvent;
import com.itranswarp.exchange.model.trade.EventEntity;
import com.itranswarp.exchange.messaging.MessageConsumer;
import com.itranswarp.exchange.messaging.MessageProducer;
import com.itranswarp.exchange.messaging.Messaging;
import com.itranswarp.exchange.messaging.MessagingFactory;
import com.itranswarp.exchange.support.LoggerSupport;
import com.itranswarp.exchange.util.JsonUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 定序服务。
 * <p>
 * 消费 SEQUENCE 主题上收到的原始事件，为每个事件分配全局唯一且严格递增的定序 ID，
 * 记录 previousId 以便下游检测消息连续性，持久化后再发送至 TRADE 主题供交易引擎消费。
 * <p>
 * 定序是整个系统保证"确定性"的关键：所有交易引擎实例消费同一条有序事件流，即可得到完全一致的状态。
 */
@Component
public class SequencerService extends LoggerSupport {

    @Autowired
    MessagingFactory messagingFactory;

    @Autowired
    EventRepository eventRepository;

    private MessageConsumer consumer;
    private MessageProducer<AbstractEvent> producer;

    /**
     * 最后分配的定序 ID。
     */
    private long lastSequenceId = 0;

    @PostConstruct
    public void init() {
        // 启动时从数据库恢复计数器:
        this.lastSequenceId = this.eventRepository.getMaxSequenceId();
        logger.info("start sequencer from last sequenceId = {}", this.lastSequenceId);
        this.producer = this.messagingFactory.createMessageProducer(Messaging.Topic.TRADE);
        this.consumer = this.messagingFactory.createBatchMessageListener(Messaging.Topic.SEQUENCE, "sequencer",
                this::sequenceMessages);
    }

    @PreDestroy
    public void destroy() {
        if (this.consumer != null) {
            this.consumer.stop();
        }
    }

    /**
     * 对一批事件定序：单线程串行执行，保证 ID 全局有序。
     */
    @Transactional
    public void sequenceMessages(List<AbstractEvent> messages) {
        List<AbstractEvent> sequenced = new ArrayList<>(messages.size());
        List<EventEntity> events = new ArrayList<>(messages.size());
        for (AbstractEvent message : messages) {
            long previousId = this.lastSequenceId;
            this.lastSequenceId++;
            message.previousId = previousId;
            message.sequenceId = this.lastSequenceId;

            EventEntity event = new EventEntity();
            event.sequenceId = message.sequenceId;
            event.previousId = previousId;
            event.data = JsonUtil.writeJson(message);
            event.createdAt = message.createdAt > 0 ? message.createdAt : System.currentTimeMillis();
            events.add(event);
            sequenced.add(message);
        }
        // 先持久化 (故障可恢复)，再发送至交易引擎:
        this.eventRepository.saveAll(events);
        this.producer.sendMessages(sequenced);
        if (logger.isDebugEnabled()) {
            logger.debug("sequenced {} messages, last sequenceId = {}", sequenced.size(), this.lastSequenceId);
        }
    }
}
