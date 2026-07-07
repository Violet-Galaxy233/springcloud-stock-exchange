package com.itranswarp.exchange.tradingengine;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.message.event.AbstractEvent;
import com.itranswarp.exchange.message.event.OrderRequestEvent;
import com.itranswarp.exchange.message.event.TransferEvent;
import com.itranswarp.exchange.model.trade.EventEntity;
import com.itranswarp.exchange.tradingengine.assets.AssetService;
import com.itranswarp.exchange.tradingengine.clearing.ClearingService;
import com.itranswarp.exchange.tradingengine.match.MatchEngine;
import com.itranswarp.exchange.tradingengine.order.OrderService;
import com.itranswarp.exchange.util.JsonUtil;

/**
 * 故障恢复测试：验证"确定性状态机"特性 —— 从持久化的事件序列重放，
 * 可精确重建到与在线处理完全相同的状态。
 */
class EngineRecoveryTest {

    static final Long DEBT = 1L;
    static final Long USER_A = 100L;
    static final Long USER_B = 200L;

    long seq = 0;

    @Test
    void replayReproducesIdenticalState() {
        // 构造一段事件：充值 + 多笔撮合 + 一笔挂单未成交:
        List<AbstractEvent> events = new ArrayList<>();
        events.add(deposit(USER_A, AssetEnum.USD, "100000"));
        events.add(deposit(USER_B, AssetEnum.BTC, "100"));
        events.add(order(USER_A, Direction.BUY, "20000", "2"));
        events.add(order(USER_B, Direction.SELL, "20000", "1"));   // 部分成交 A
        events.add(order(USER_B, Direction.SELL, "19000", "1"));   // 以更低价再成交 A
        events.add(order(USER_A, Direction.BUY, "18000", "1"));    // 挂单未成交

        // 引擎 1：在线处理:
        Ctx live = newEngine();
        live.engine.processMessages(new ArrayList<>(events));

        // 将事件持久化为 EventEntity (与定序服务写库格式一致):
        List<EventEntity> stored = new ArrayList<>();
        for (AbstractEvent ev : events) {
            EventEntity e = new EventEntity();
            e.sequenceId = ev.sequenceId;
            e.previousId = ev.previousId;
            e.data = JsonUtil.writeJson(ev);
            e.createdAt = ev.createdAt;
            stored.add(e);
        }

        // 引擎 2：从事件序列重放恢复:
        Ctx recovered = newEngine();
        recovered.engine.recover(stored);

        // 两者状态应完全一致:
        for (Long user : List.of(DEBT, USER_A, USER_B)) {
            for (AssetEnum asset : AssetEnum.values()) {
                assertEquals(available(live, user, asset), available(recovered, user, asset),
                        "available mismatch user=" + user + " asset=" + asset);
                assertEquals(frozen(live, user, asset), frozen(recovered, user, asset),
                        "frozen mismatch user=" + user + " asset=" + asset);
            }
        }
        // 最新市场价与订单簿深度一致:
        assertEquals(live.engine.getMarketPrice(), recovered.engine.getMarketPrice());
        assertEquals(live.engine.getOrderBook(10).buy.size(), recovered.engine.getOrderBook(10).buy.size());
        assertEquals(live.engine.getUserOrders(USER_A).size(), recovered.engine.getUserOrders(USER_A).size());
    }

    // ---------- helpers ----------

    static class Ctx {
        AssetService assetService;
        TradingEngineService engine;
    }

    Ctx newEngine() {
        Ctx c = new Ctx();
        c.assetService = new AssetService();
        OrderService orderService = new OrderService(c.assetService);
        MatchEngine matchEngine = new MatchEngine();
        ClearingService clearingService = new ClearingService(c.assetService, orderService);
        c.engine = new TradingEngineService(c.assetService, orderService, matchEngine, clearingService);
        return c;
    }

    BigDecimal available(Ctx c, Long userId, AssetEnum asset) {
        var a = c.assetService.getAsset(userId, asset);
        return a == null ? BigDecimal.ZERO : a.getAvailable();
    }

    BigDecimal frozen(Ctx c, Long userId, AssetEnum asset) {
        var a = c.assetService.getAsset(userId, asset);
        return a == null ? BigDecimal.ZERO : a.getFrozen();
    }

    TransferEvent deposit(Long userId, AssetEnum asset, String amount) {
        TransferEvent e = new TransferEvent();
        stamp(e);
        e.fromUserId = DEBT;
        e.toUserId = userId;
        e.asset = asset;
        e.amount = new BigDecimal(amount);
        e.sufficient = false;
        return e;
    }

    OrderRequestEvent order(Long userId, Direction direction, String price, String quantity) {
        OrderRequestEvent e = new OrderRequestEvent();
        stamp(e);
        e.userId = userId;
        e.direction = direction;
        e.price = new BigDecimal(price);
        e.quantity = new BigDecimal(quantity);
        return e;
    }

    void stamp(AbstractEvent e) {
        e.previousId = this.seq;
        this.seq++;
        e.sequenceId = this.seq;
        e.createdAt = 1_000_000L + this.seq;
    }
}
