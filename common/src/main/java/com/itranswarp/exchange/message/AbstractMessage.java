package com.itranswarp.exchange.message;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 所有消息的基类。
 * <p>
 * 通过 Jackson 的多态类型信息 (@class) 序列化，使得消费端可以用
 * {@code objectMapper.readValue(json, AbstractMessage.class)} 还原出具体子类型。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class AbstractMessage {

    /**
     * 消息创建时间 (毫秒)。
     */
    public long createdAt;

    /**
     * 关联的引用 ID (如客户端请求的唯一 ID)，用于去重与关联，可为空。
     */
    public String refId;
}
