package com.itranswarp.exchange.tradingapi.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itranswarp.exchange.ApiError;
import com.itranswarp.exchange.ApiException;
import com.itranswarp.exchange.bean.OrderBookBean;
import com.itranswarp.exchange.ctx.UserContext;
import com.itranswarp.exchange.message.event.OrderCancelEvent;
import com.itranswarp.exchange.message.event.OrderRequestEvent;
import com.itranswarp.exchange.message.event.TransferEvent;
import com.itranswarp.exchange.model.trade.MatchDetailEntity;
import com.itranswarp.exchange.model.trade.OrderEntity;
import com.itranswarp.exchange.redis.RedisCache;
import com.itranswarp.exchange.redis.RedisService;
import com.itranswarp.exchange.tradingapi.SendEventService;
import com.itranswarp.exchange.tradingapi.bean.OrderRequest;
import com.itranswarp.exchange.tradingapi.bean.TransferRequest;
import com.itranswarp.exchange.tradingapi.engine.TradingEngineApiClient;
import com.itranswarp.exchange.tradingapi.store.HistoryOrderRepository;
import com.itranswarp.exchange.tradingapi.store.MatchDetailRepository;
import com.itranswarp.exchange.util.JsonUtil;

/**
 * 交易 API：交易员下单、撤单、充值 (演示) 的唯一入口，以及订单簿、资产、订单的查询。
 * <p>
 * 下单/撤单/充值等"写"操作构造事件发往定序服务 (Kafka)，异步处理；
 * 订单簿/资产/活动订单等"读"操作直接查询交易引擎或 Redis 快照。
 */
@RestController
@RequestMapping("/api")
public class TradingApiController {

    /**
     * 系统负债账户 ID，充值时由该账户向用户转账。
     */
    static final Long DEBT_USER_ID = 1L;

    final SendEventService sendEventService;
    final TradingEngineApiClient engineApiClient;
    final RedisService redisService;
    final HistoryOrderRepository historyOrderRepository;
    final MatchDetailRepository matchDetailRepository;

    public TradingApiController(SendEventService sendEventService, TradingEngineApiClient engineApiClient,
            RedisService redisService, HistoryOrderRepository historyOrderRepository,
            MatchDetailRepository matchDetailRepository) {
        this.sendEventService = sendEventService;
        this.engineApiClient = engineApiClient;
        this.redisService = redisService;
        this.historyOrderRepository = historyOrderRepository;
        this.matchDetailRepository = matchDetailRepository;
    }

    // ---------- 查询 (读) ----------

    @GetMapping("/orderBook")
    public OrderBookBean getOrderBook() {
        // 优先读取 Redis 中的订单簿快照:
        String snapshot = this.redisService.get(RedisCache.OrderBook);
        if (snapshot != null && !snapshot.isEmpty()) {
            return JsonUtil.readJson(snapshot, OrderBookBean.class);
        }
        // 回退：直接查询交易引擎:
        return this.engineApiClient.getOrderBook();
    }

    @GetMapping("/marketPrice")
    public Map<String, Object> getMarketPrice() {
        return this.engineApiClient.getMarketPrice();
    }

    @GetMapping("/assets")
    public Map<String, Object> getAssets() {
        Long userId = UserContext.getRequiredUserId();
        return this.engineApiClient.getUserAssets(userId);
    }

    @GetMapping("/orders")
    public List<OrderEntity> getOrders() {
        Long userId = UserContext.getRequiredUserId();
        return this.engineApiClient.getUserOrders(userId);
    }

    @GetMapping("/orders/{orderId}")
    public OrderEntity getOrder(@PathVariable("orderId") Long orderId) {
        Long userId = UserContext.getRequiredUserId();
        OrderEntity order = this.engineApiClient.getOrder(orderId);
        if (order == null || !order.userId.equals(userId)) {
            throw new ApiException(ApiError.NOT_FOUND, "Order not found.");
        }
        return order;
    }

    @GetMapping("/history/orders")
    public List<OrderEntity> getHistoryOrders() {
        Long userId = UserContext.getRequiredUserId();
        return this.historyOrderRepository.findTop100ByUserIdOrderByIdDesc(userId);
    }

    @GetMapping("/history/orders/{orderId}/matches")
    public List<MatchDetailEntity> getOrderMatches(@PathVariable("orderId") Long orderId) {
        UserContext.getRequiredUserId();
        return this.matchDetailRepository.findByOrderIdOrderByIdAsc(orderId);
    }

    // ---------- 命令 (写) ----------

    @PostMapping("/orders")
    public Map<String, String> createOrder(@RequestBody OrderRequest req) {
        Long userId = UserContext.getRequiredUserId();
        if (req == null || req.direction == null || req.price == null || req.quantity == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "direction, price and quantity are required.");
        }
        if (req.price.signum() <= 0 || req.quantity.signum() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "price and quantity must be positive.");
        }
        OrderRequestEvent event = new OrderRequestEvent();
        event.refId = UUID.randomUUID().toString();
        event.userId = userId;
        event.direction = req.direction;
        event.price = req.price;
        event.quantity = req.quantity;
        event.createdAt = System.currentTimeMillis();
        this.sendEventService.sendEvent(event);
        return Map.of("status", "accepted", "refId", event.refId);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public Map<String, String> cancelOrder(@PathVariable("orderId") Long orderId) {
        Long userId = UserContext.getRequiredUserId();
        OrderCancelEvent event = new OrderCancelEvent();
        event.refId = UUID.randomUUID().toString();
        event.userId = userId;
        event.refOrderId = orderId;
        event.createdAt = System.currentTimeMillis();
        this.sendEventService.sendEvent(event);
        return Map.of("status", "accepted", "refId", event.refId);
    }

    /**
     * 演示用充值：由系统负债账户向当前用户转入资产。真实系统应对接银行/区块链并鉴权。
     */
    @PostMapping("/transfer")
    public Map<String, String> transfer(@RequestBody TransferRequest req) {
        Long userId = UserContext.getRequiredUserId();
        if (req == null || req.asset == null || req.amount == null || req.amount.signum() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "asset and positive amount are required.");
        }
        TransferEvent event = new TransferEvent();
        event.refId = UUID.randomUUID().toString();
        event.fromUserId = DEBT_USER_ID;
        event.toUserId = userId;
        event.asset = req.asset;
        event.amount = req.amount;
        event.sufficient = false;
        event.createdAt = System.currentTimeMillis();
        this.sendEventService.sendEvent(event);
        return Map.of("status", "accepted", "refId", event.refId);
    }
}
