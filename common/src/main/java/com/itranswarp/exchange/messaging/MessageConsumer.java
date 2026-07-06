package com.itranswarp.exchange.messaging;

/**
 * 消息消费者句柄，可用于停止底层监听容器。
 */
public interface MessageConsumer {

    void stop();
}
