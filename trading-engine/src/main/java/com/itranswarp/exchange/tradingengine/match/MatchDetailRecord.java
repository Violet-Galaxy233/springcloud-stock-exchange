package com.itranswarp.exchange.tradingengine.match;

import java.math.BigDecimal;

import com.itranswarp.exchange.model.trade.OrderEntity;

/**
 * 一条撮合明细：taker 订单与 maker 订单以某价格成交某数量。
 */
public record MatchDetailRecord(BigDecimal price, BigDecimal quantity, OrderEntity takerOrder, OrderEntity makerOrder) {
}
