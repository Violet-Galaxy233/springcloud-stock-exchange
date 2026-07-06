package com.itranswarp.exchange.messaging;

/**
 * 消息主题定义。
 */
public interface Messaging {

    enum Topic {

        /**
         * 定序主题：交易 API 将原始事件发送至此，由定序服务消费。分区数固定为 1 以保证全局有序。
         */
        SEQUENCE(1),

        /**
         * 交易主题：定序服务将定序后的事件发送至此，由交易引擎消费。
         */
        TRADE(1),

        /**
         * 行情主题：交易引擎将 Tick 发送至此，由行情服务消费。
         */
        TICK(1);

        private final int partitions;

        Topic(int partitions) {
            this.partitions = partitions;
        }

        public int getPartitions() {
            return this.partitions;
        }
    }
}
