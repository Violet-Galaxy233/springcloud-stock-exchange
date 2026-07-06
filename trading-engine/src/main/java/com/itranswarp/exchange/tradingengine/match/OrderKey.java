package com.itranswarp.exchange.tradingengine.match;

import java.math.BigDecimal;

/**
 * 订单簿排序键：价格 + 定序 ID。
 */
public record OrderKey(long sequenceId, BigDecimal price) {
}
