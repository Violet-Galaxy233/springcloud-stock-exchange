package com.itranswarp.exchange.tradingengine.match;

import java.util.ArrayList;
import java.util.List;

import com.itranswarp.exchange.model.trade.OrderEntity;

/**
 * 一次撮合的结果：taker 订单 + 若干条与 maker 的成交明细。
 * 清算系统据此完成买卖双方资产交换。
 */
public class MatchResult {

    public final OrderEntity takerOrder;
    public final List<MatchDetailRecord> matchDetails = new ArrayList<>();

    public MatchResult(OrderEntity takerOrder) {
        this.takerOrder = takerOrder;
    }

    public void add(java.math.BigDecimal price, java.math.BigDecimal matchedQuantity, OrderEntity makerOrder) {
        this.matchDetails.add(new MatchDetailRecord(price, matchedQuantity, this.takerOrder, makerOrder));
    }

    public boolean isEmpty() {
        return this.matchDetails.isEmpty();
    }

    @Override
    public String toString() {
        if (this.matchDetails.isEmpty()) {
            return "no match";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.matchDetails.size()).append(" matches:");
        for (MatchDetailRecord r : this.matchDetails) {
            sb.append("\n  ").append(r.quantity()).append("@").append(r.price());
        }
        return sb.toString();
    }
}
