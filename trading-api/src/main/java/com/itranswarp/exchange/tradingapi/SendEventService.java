package com.itranswarp.exchange.tradingapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.itranswarp.exchange.message.event.AbstractEvent;
import com.itranswarp.exchange.messaging.MessageProducer;
import com.itranswarp.exchange.messaging.Messaging;
import com.itranswarp.exchange.messaging.MessagingFactory;
import com.itranswarp.exchange.support.LoggerSupport;

import jakarta.annotation.PostConstruct;

/**
 * 将业务事件 (下单/撤单/转账) 发送至定序服务 (SEQUENCE 主题) 的统一入口。
 */
@Component
public class SendEventService extends LoggerSupport {

    @Autowired
    MessagingFactory messagingFactory;

    private MessageProducer<AbstractEvent> producer;

    @PostConstruct
    public void init() {
        this.producer = this.messagingFactory.createMessageProducer(Messaging.Topic.SEQUENCE);
    }

    public void sendEvent(AbstractEvent event) {
        if (event.createdAt <= 0) {
            event.createdAt = System.currentTimeMillis();
        }
        this.producer.sendMessage(event);
        if (logger.isDebugEnabled()) {
            logger.debug("send event to sequencer: {}", event);
        }
    }
}
