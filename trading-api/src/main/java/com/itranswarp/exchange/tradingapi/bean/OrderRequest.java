package com.itranswarp.exchange.tradingapi.bean;

import java.math.BigDecimal;

import com.itranswarp.exchange.enums.Direction;

/**
 * 下单请求体。
 */
public class OrderRequest {

    public Direction direction;
    public BigDecimal price;
    public BigDecimal quantity;
}
