package com.itranswarp.exchange.message.event;

import java.math.BigDecimal;

import com.itranswarp.exchange.enums.Direction;

/**
 * 下单请求事件。
 */
public class OrderRequestEvent extends AbstractEvent {

    public Long userId;
    public Direction direction;
    public BigDecimal price;
    public BigDecimal quantity;

    @Override
    public String toString() {
        return "OrderRequestEvent [sequenceId=" + sequenceId + ", userId=" + userId + ", direction=" + direction
                + ", price=" + price + ", quantity=" + quantity + "]";
    }
}
