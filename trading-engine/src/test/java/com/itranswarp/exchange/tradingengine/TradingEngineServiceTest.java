package com.itranswarp.exchange.tradingengine;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.message.event.AbstractEvent;
import com.itranswarp.exchange.message.event.OrderCancelEvent;
import com.itranswarp.exchange.message.event.OrderRequestEvent;
import com.itranswarp.exchange.message.event.TransferEvent;
import com.itranswarp.exchange.model.trade.OrderEntity;
import com.itranswarp.exchange.tradingengine.assets.Asset;
import com.itranswarp.exchange.tradingengine.assets.AssetService;
import com.itranswarp.exchange.tradingengine.clearing.ClearingService;
import com.itranswarp.exchange.tradingengine.match.MatchEngine;
import com.itranswarp.exchange.tradingengine.order.OrderService;

/**
 * 交易引擎端到端测试：从充值到下单、撮合、清算，验证资产变化与系统资产守恒。
 * <p>
 * 全部使用内存态运行，不依赖 Kafka/Redis/MySQL。
 */
class TradingEngineServiceTest {

    static final Long DEBT = 1L;
    static final Long USER_A = 100L;
    static final Long USER_B = 200L;

    AssetService assetService;
    TradingEngineService engine;

    long seq = 0;

    @BeforeEach
    void setUp() {
        assetService = new AssetService();
        OrderService orderService = new OrderService(assetService);
        MatchEngine matchEngine = new MatchEngine();
        ClearingService clearingService = new ClearingService(assetService, orderService);
        engine = new TradingEngineService(assetService, orderService, matchEngine, clearingService);
    }

    @Test
    void fullTradeFlowConservesAssets() {
        List<AbstractEvent> events = new ArrayList<>();
        // 充值:
        events.add(deposit(USER_A, AssetEnum.USD, "100000"));
        events.add(deposit(USER_B, AssetEnum.BTC, "100"));
        // A 买入 1 BTC @ 20000:
        events.add(buy(USER_A, "20000", "1"));
        // B 卖出 1 BTC @ 20000 -> 成交:
        events.add(sell(USER_B, "20000", "1"));

        engine.processMessages(events);

        // A: USD 可用 80000, BTC 可用 1:
        assertEquals(bd("80000"), avail(USER_A, AssetEnum.USD));
        assertEquals(0, frozen(USER_A, AssetEnum.USD).signum());
        assertEquals(bd("1"), avail(USER_A, AssetEnum.BTC));
        // B: USD 可用 20000, BTC 可用 99:
        assertEquals(bd("20000"), avail(USER_B, AssetEnum.USD));
        assertEquals(bd("99"), avail(USER_B, AssetEnum.BTC));
        assertEquals(0, frozen(USER_B, AssetEnum.BTC).signum());

        assertConservation();
    }

    @Test
    void buyerGetsRefundWhenMatchedCheaper() {
        List<AbstractEvent> events = new ArrayList<>();
        events.add(deposit(USER_A, AssetEnum.USD, "100000"));
        events.add(deposit(USER_B, AssetEnum.BTC, "100"));
        // B 先挂卖单 1 BTC @ 19000:
        events.add(sell(USER_B, "19000", "1"));
        // A 出价 20000 买入 1 BTC，应以 19000 成交，多冻结的 1000 退回:
        events.add(buy(USER_A, "20000", "1"));

        engine.processMessages(events);

        // A 花费 19000, 剩余可用 81000, 无冻结:
        assertEquals(bd("81000"), avail(USER_A, AssetEnum.USD));
        assertEquals(0, frozen(USER_A, AssetEnum.USD).signum());
        assertEquals(bd("1"), avail(USER_A, AssetEnum.BTC));
        // B 获得 19000 USD:
        assertEquals(bd("19000"), avail(USER_B, AssetEnum.USD));
        assertConservation();
    }

    @Test
    void cancelUnfreezesAssets() {
        List<AbstractEvent> events = new ArrayList<>();
        events.add(deposit(USER_A, AssetEnum.USD, "100000"));
        long buySeq = this.seq + 1; // 下一个事件的定序 ID 即订单 ID
        events.add(buy(USER_A, "20000", "1"));
        engine.processMessages(events);

        // 下单后冻结 20000:
        assertEquals(bd("80000"), avail(USER_A, AssetEnum.USD));
        assertEquals(bd("20000"), frozen(USER_A, AssetEnum.USD));

        // 撤单:
        engine.processMessages(List.of(cancel(USER_A, buySeq)));

        // 撤单后全额解冻:
        assertEquals(bd("100000"), avail(USER_A, AssetEnum.USD));
        assertEquals(0, frozen(USER_A, AssetEnum.USD).signum());
        // 活动订单已移除:
        assertNull(engine.getOrder(buySeq));
        assertConservation();
    }

    @Test
    void insufficientBalanceRejectsOrder() {
        List<AbstractEvent> events = new ArrayList<>();
        events.add(deposit(USER_A, AssetEnum.USD, "100"));
        // 试图买入需要 20000 USD，余额不足，订单被拒:
        events.add(buy(USER_A, "20000", "1"));
        engine.processMessages(events);

        // 余额不变，无冻结:
        assertEquals(bd("100"), avail(USER_A, AssetEnum.USD));
        assertEquals(0, frozen(USER_A, AssetEnum.USD).signum());
        assertConservation();
    }

    // ---------- helpers ----------

    TransferEvent deposit(Long userId, AssetEnum asset, String amount) {
        TransferEvent e = new TransferEvent();
        stamp(e);
        e.fromUserId = DEBT;
        e.toUserId = userId;
        e.asset = asset;
        e.amount = bd(amount);
        e.sufficient = false;
        return e;
    }

    OrderRequestEvent buy(Long userId, String price, String quantity) {
        return orderRequest(userId, Direction.BUY, price, quantity);
    }

    OrderRequestEvent sell(Long userId, String price, String quantity) {
        return orderRequest(userId, Direction.SELL, price, quantity);
    }

    OrderRequestEvent orderRequest(Long userId, Direction direction, String price, String quantity) {
        OrderRequestEvent e = new OrderRequestEvent();
        stamp(e);
        e.userId = userId;
        e.direction = direction;
        e.price = bd(price);
        e.quantity = bd(quantity);
        return e;
    }

    OrderCancelEvent cancel(Long userId, long refOrderId) {
        OrderCancelEvent e = new OrderCancelEvent();
        stamp(e);
        e.userId = userId;
        e.refOrderId = refOrderId;
        return e;
    }

    void stamp(AbstractEvent e) {
        e.previousId = this.seq;
        this.seq++;
        e.sequenceId = this.seq;
        e.createdAt = 1_000_000L + this.seq;
    }

    BigDecimal avail(Long userId, AssetEnum asset) {
        return assetService.getAsset(userId, asset).getAvailable();
    }

    BigDecimal frozen(Long userId, AssetEnum asset) {
        Asset a = assetService.getAsset(userId, asset);
        return a == null ? BigDecimal.ZERO : a.getFrozen();
    }

    void assertConservation() {
        for (AssetEnum asset : AssetEnum.values()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (Map.Entry<Long, ConcurrentMap<AssetEnum, Asset>> e : assetService.getUserAssets().entrySet()) {
                Asset a = e.getValue().get(asset);
                if (a != null) {
                    sum = sum.add(a.getAvailable()).add(a.getFrozen());
                }
            }
            assertEquals(0, sum.signum(), "Asset " + asset + " total must be zero but was " + sum);
        }
    }

    static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
