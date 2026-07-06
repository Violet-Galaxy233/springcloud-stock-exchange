package com.itranswarp.exchange.message.event;

/**
 * 撤单请求事件。
 */
public class OrderCancelEvent extends AbstractEvent {

    public Long userId;

    /**
     * 待撤销的订单 ID。
     */
    public Long refOrderId;

    @Override
    public String toString() {
        return "OrderCancelEvent [sequenceId=" + sequenceId + ", userId=" + userId + ", refOrderId=" + refOrderId + "]";
    }
}
