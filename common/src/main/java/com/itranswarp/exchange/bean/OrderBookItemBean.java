package com.itranswarp.exchange.bean;

import java.math.BigDecimal;

/**
 * 订单簿中的一档：某价格上的挂单总量。
 */
public class OrderBookItemBean {

    public BigDecimal price;
    public BigDecimal quantity;

    public OrderBookItemBean() {
    }

    public OrderBookItemBean(BigDecimal price, BigDecimal quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public void addQuantity(BigDecimal quantity) {
        this.quantity = this.quantity.add(quantity);
    }
}
