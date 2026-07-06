package com.itranswarp.exchange.tradingengine.match;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itranswarp.exchange.bean.OrderBookBean;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.OrderStatus;
import com.itranswarp.exchange.model.trade.OrderEntity;

/**
 * 撮合引擎测试：验证"价格优先、时间优先"与以 maker 价成交。
 */
class MatchEngineTest {

    MatchEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MatchEngine();
    }

    @Test
    void restingOrdersEnterBook() {
        engine.processOrder(1, order(1, Direction.SELL, "100", "3"));
        engine.processOrder(2, order(2, Direction.SELL, "101", "2"));
        // 无对手盘，全部进入卖盘:
        assertEquals(2, engine.sellBook.size());
        assertEquals(0, engine.buyBook.size());
        // 卖盘最优价应为 100:
        assertEquals(bd("100"), engine.sellBook.getFirst().price);
    }

    @Test
    void takerMatchesAcrossPriceLevels() {
        engine.processOrder(1, order(1, Direction.SELL, "100", "3"));
        engine.processOrder(2, order(2, Direction.SELL, "101", "2"));
        // 买入 4 @ 101，应吃掉 3@100 与 1@101:
        MatchResult result = engine.processOrder(3, order(3, Direction.BUY, "101", "4"));
        assertEquals(2, result.matchDetails.size());
        // 以 maker 挂单价成交:
        assertEquals(bd("100"), result.matchDetails.get(0).price());
        assertEquals(bd("3"), result.matchDetails.get(0).quantity());
        assertEquals(bd("101"), result.matchDetails.get(1).price());
        assertEquals(bd("1"), result.matchDetails.get(1).quantity());
        // 最新市场价为最后一次成交价:
        assertEquals(bd("101"), engine.marketPrice);
        // taker 完全成交:
        assertEquals(OrderStatus.FULLY_FILLED, result.takerOrder.status);
        // 卖盘剩下 seq2 未成交 1:
        assertEquals(1, engine.sellBook.size());
        assertEquals(bd("1"), engine.sellBook.getFirst().unfilledQuantity);
        assertEquals(0, engine.buyBook.size());
    }

    @Test
    void unmatchedRemainderRestsInBook() {
        engine.processOrder(1, order(1, Direction.SELL, "100", "1"));
        // 买入 5 @ 100，只能成交 1，剩余 4 挂在买盘:
        MatchResult result = engine.processOrder(2, order(2, Direction.BUY, "100", "5"));
        assertEquals(1, result.matchDetails.size());
        assertEquals(OrderStatus.PARTIAL_FILLED, result.takerOrder.status);
        assertEquals(bd("4"), result.takerOrder.unfilledQuantity);
        assertEquals(0, engine.sellBook.size());
        assertEquals(1, engine.buyBook.size());
    }

    @Test
    void priceTimePriority() {
        // 相同价格，先到的先成交:
        engine.processOrder(1, order(1, Direction.BUY, "100", "1"));
        engine.processOrder(2, order(2, Direction.BUY, "100", "1"));
        // 卖出 1 @ 100，应与 seq1 (更早) 成交:
        MatchResult result = engine.processOrder(3, order(3, Direction.SELL, "100", "1"));
        assertEquals(1, result.matchDetails.size());
        assertEquals(1L, result.matchDetails.get(0).makerOrder().sequenceId);
    }

    @Test
    void orderBookSnapshotAggregatesByPrice() {
        engine.processOrder(1, order(1, Direction.BUY, "100", "1"));
        engine.processOrder(2, order(2, Direction.BUY, "100", "2"));
        engine.processOrder(3, order(3, Direction.BUY, "99", "5"));
        OrderBookBean book = engine.getOrderBook(10);
        // 买盘两档: 100 (聚合数量 3), 99 (数量 5):
        assertEquals(2, book.buy.size());
        assertEquals(bd("100"), book.buy.get(0).price);
        assertEquals(bd("3"), book.buy.get(0).quantity);
        assertEquals(bd("99"), book.buy.get(1).price);
    }

    static OrderEntity order(long seq, Direction direction, String price, String quantity) {
        OrderEntity o = new OrderEntity();
        o.id = seq;
        o.sequenceId = seq;
        o.userId = seq;
        o.direction = direction;
        o.price = bd(price);
        o.quantity = bd(quantity);
        o.unfilledQuantity = bd(quantity);
        o.status = OrderStatus.PENDING;
        o.createdAt = o.updatedAt = seq;
        return o;
    }

    static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
