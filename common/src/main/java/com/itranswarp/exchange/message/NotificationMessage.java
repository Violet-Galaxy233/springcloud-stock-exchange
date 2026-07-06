package com.itranswarp.exchange.message;

/**
 * 推送通知消息。交易引擎通过 Redis 发布，推送服务 (Push) 订阅后经 WebSocket 转发给对应用户。
 */
public class NotificationMessage extends AbstractMessage {

    /**
     * 通知类型，如 order_matched / order_canceled 等。
     */
    public String type;

    /**
     * 目标用户 ID，为空表示广播 (如市场行情)。
     */
    public Long userId;

    /**
     * 通知携带的数据。
     */
    public Object data;
}
