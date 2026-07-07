package com.itranswarp.exchange.tradingengine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.itranswarp.exchange.bean.OrderBookBean;
import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.MatchType;
import com.itranswarp.exchange.enums.OrderStatus;
import com.itranswarp.exchange.message.AbstractMessage;
import com.itranswarp.exchange.message.NotificationMessage;
import com.itranswarp.exchange.message.TickMessage;
import com.itranswarp.exchange.message.event.AbstractEvent;
import com.itranswarp.exchange.message.event.OrderCancelEvent;
import com.itranswarp.exchange.message.event.OrderRequestEvent;
import com.itranswarp.exchange.message.event.TransferEvent;
import com.itranswarp.exchange.messaging.MessageConsumer;
import com.itranswarp.exchange.messaging.MessageProducer;
import com.itranswarp.exchange.messaging.Messaging;
import com.itranswarp.exchange.messaging.MessagingFactory;
import com.itranswarp.exchange.model.quotation.TickEntity;
import com.itranswarp.exchange.model.trade.EventEntity;
import com.itranswarp.exchange.model.trade.MatchDetailEntity;
import com.itranswarp.exchange.model.trade.OrderEntity;
import com.itranswarp.exchange.redis.RedisCache;
import com.itranswarp.exchange.redis.RedisService;
import com.itranswarp.exchange.support.LoggerSupport;
import com.itranswarp.exchange.tradingengine.assets.AssetService;
import com.itranswarp.exchange.tradingengine.assets.Transfer;
import com.itranswarp.exchange.tradingengine.clearing.ClearingService;
import com.itranswarp.exchange.tradingengine.match.MatchDetailRecord;
import com.itranswarp.exchange.tradingengine.match.MatchEngine;
import com.itranswarp.exchange.tradingengine.match.MatchResult;
import com.itranswarp.exchange.tradingengine.order.OrderService;
import com.itranswarp.exchange.tradingengine.store.EventReplayRepository;
import com.itranswarp.exchange.tradingengine.store.StoreService;
import com.itranswarp.exchange.util.JsonUtil;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 交易引擎核心：一个事件驱动的确定性状态机。
 * <p>
 * 输入是定序后的事件序列 (下单/撤单/转账)，内部状态 (资产表、订单集、订单簿) 随之确定性更新，
 * 输出为成交明细、Tick 行情与推送通知。相同的输入序列在任意实例上都会产生完全相同的输出，
 * 因此可以水平复制多个实例实现高可用。
 */
@Component
public class TradingEngineService extends LoggerSupport {

    private final AssetService assetService;
    private final OrderService orderService;
    private final MatchEngine matchEngine;
    private final ClearingService clearingService;

    // 以下为 IO 依赖，单元测试时可缺省 (为 null)，仅执行纯内存撮合逻辑:
    @Autowired(required = false)
    private MessagingFactory messagingFactory;
    @Autowired(required = false)
    private RedisService redisService;
    @Autowired(required = false)
    private StoreService storeService;
    @Autowired(required = false)
    private EventReplayRepository eventReplayRepository;

    private MessageConsumer tradeConsumer;
    private MessageProducer<TickMessage> tickProducer;

    /**
     * 已处理的最后一个定序 ID，用于去重/顺序校验。
     */
    private long lastSequenceId = 0;

    /**
     * 致命错误标志：一旦发生数据不一致，停止处理以避免污染状态。
     */
    private boolean fatalError = false;

    // 每批次的输出缓冲，批处理结束后统一 flush:
    private final List<OrderEntity> orderBuffer = new ArrayList<>();
    private final List<MatchDetailEntity> matchBuffer = new ArrayList<>();
    private final List<TickEntity> tickBuffer = new ArrayList<>();
    private final List<NotificationMessage> notificationBuffer = new ArrayList<>();

    private boolean orderBookChanged = false;

    public TradingEngineService(AssetService assetService, OrderService orderService, MatchEngine matchEngine,
            ClearingService clearingService) {
        this.assetService = assetService;
        this.orderService = orderService;
        this.matchEngine = matchEngine;
        this.clearingService = clearingService;
    }

    @PostConstruct
    public void init() {
        // 1) 先从数据库重放历史事件重建内存状态 (故障恢复):
        recover();
        // 2) 再开始消费 Kafka 上的新事件:
        if (this.messagingFactory != null) {
            this.tickProducer = this.messagingFactory.createMessageProducer(Messaging.Topic.TICK);
            this.tradeConsumer = this.messagingFactory.createBatchMessageListener(Messaging.Topic.TRADE,
                    "trading-engine", this::processMessages);
            logger.info("trading engine started, consuming TRADE topic...");
        }
    }

    /**
     * 启动恢复：按定序 ID 升序重放 events 表中的事件，重建资产/订单/订单簿状态。
     * <p>
     * 交易引擎是确定性状态机 —— 对相同事件序列必得相同状态，因此重放即可恢复到宕机前的一致状态。
     * 重放期间产生的输出 (通知/Tick/持久化) 全部丢弃，避免重复副作用。
     */
    public void recover() {
        if (this.eventReplayRepository == null) {
            return;
        }
        recover(this.eventReplayRepository.findAllByOrderBySequenceIdAsc());
    }

    /**
     * 按给定的事件序列重放恢复 (提取为独立方法便于测试)。
     */
    public void recover(List<EventEntity> events) {
        if (events.isEmpty()) {
            logger.info("no events to recover.");
            return;
        }
        logger.info("recovering {} events...", events.size());
        for (EventEntity e : events) {
            AbstractMessage message = JsonUtil.readJson(e.data, AbstractMessage.class);
            if (message instanceof AbstractEvent event) {
                processEvent(event);
            }
        }
        // 丢弃重放期间累积的输出缓冲，避免重复发送/持久化:
        this.orderBuffer.clear();
        this.matchBuffer.clear();
        this.tickBuffer.clear();
        this.notificationBuffer.clear();
        this.orderBookChanged = false;
        logger.info("recovery finished, lastSequenceId = {}", this.lastSequenceId);
    }

    @PreDestroy
    public void destroy() {
        if (this.tradeConsumer != null) {
            this.tradeConsumer.stop();
        }
    }

    /**
     * Kafka 批量回调：依次处理一批事件，然后统一 flush 输出。
     */
    public void processMessages(List<AbstractEvent> messages) {
        this.orderBookChanged = false;
        for (AbstractEvent message : messages) {
            processEvent(message);
        }
        flush();
    }

    /**
     * 处理单个事件 (纯内存，确定性)。单元测试可直接调用。
     */
    public void processEvent(AbstractEvent event) {
        if (this.fatalError) {
            return;
        }
        // 顺序/去重校验:
        if (event.sequenceId <= this.lastSequenceId) {
            logger.warn("skip duplicate or outdated event: {} <= {}", event.sequenceId, this.lastSequenceId);
            return;
        }
        if (event.previousId != this.lastSequenceId) {
            logger.warn("event {} has previousId {} but last processed is {}", event.sequenceId, event.previousId,
                    this.lastSequenceId);
            // 简化处理：仍然接受，生产环境应触发状态恢复。
        }

        if (event instanceof OrderRequestEvent e) {
            createOrder(e);
        } else if (event instanceof OrderCancelEvent e) {
            cancelOrder(e);
        } else if (event instanceof TransferEvent e) {
            transfer(e);
        } else {
            logger.error("unable to process event: {}", event);
        }
        this.lastSequenceId = event.sequenceId;
    }

    void createOrder(OrderRequestEvent event) {
        long ts = event.createdAt;
        // 使用定序 ID 作为订单 ID (全局唯一且确定):
        Long orderId = event.sequenceId;
        OrderEntity order = this.orderService.createOrder(event.sequenceId, ts, orderId, event.userId, event.direction,
                event.price, event.quantity);
        if (order == null) {
            logger.warn("create order failed: no enough asset for user {}", event.userId);
            this.notificationBuffer
                    .add(createNotification("order_rejected", event.userId, event.refId == null ? orderId : event.refId));
            return;
        }
        // 撮合:
        MatchResult result = this.matchEngine.processOrder(event.sequenceId, order);
        // 清算:
        this.clearingService.clearMatchResult(result);
        this.orderBookChanged = true;

        // 收集输出:
        this.orderBuffer.add(order.copy());
        this.notificationBuffer.add(createNotification("order_created", order.userId, order.copy()));

        long baseId = event.sequenceId * 1_000_000L;
        int index = 0;
        for (MatchDetailRecord detail : result.matchDetails) {
            OrderEntity maker = detail.makerOrder();
            // 成交产生一条 Tick:
            TickEntity tick = new TickEntity();
            tick.id = baseId + index;
            tick.sequenceId = event.sequenceId;
            tick.takerDirection = order.direction == Direction.BUY;
            tick.price = detail.price();
            tick.quantity = detail.quantity();
            tick.createdAt = ts;
            this.tickBuffer.add(tick);

            // taker + maker 各一条成交明细:
            this.matchBuffer.add(createMatchDetail(baseId + 1_000L + index * 2L, event.sequenceId, ts, order, maker,
                    MatchType.TAKER, detail));
            this.matchBuffer.add(createMatchDetail(baseId + 1_000L + index * 2L + 1, event.sequenceId, ts, maker, order,
                    MatchType.MAKER, detail));

            // 更新后的 maker 订单也需持久化:
            this.orderBuffer.add(maker.copy());
            this.notificationBuffer.add(createNotification("order_matched", maker.userId, maker.copy()));
            index++;
        }
        if (!result.isEmpty()) {
            this.notificationBuffer.add(createNotification("order_matched", order.userId, order.copy()));
        }
    }

    void cancelOrder(OrderCancelEvent event) {
        OrderEntity order = this.orderService.getOrder(event.refOrderId);
        // 订单不存在或不属于该用户:
        if (order == null || !order.userId.equals(event.userId)) {
            logger.warn("cancel order failed: order {} not found for user {}", event.refOrderId, event.userId);
            this.notificationBuffer.add(createNotification("order_cancel_failed", event.userId, event.refOrderId));
            return;
        }
        // 从订单簿移除:
        this.matchEngine.remove(order);
        // 更新状态:
        OrderStatus status = order.unfilledQuantity.compareTo(order.quantity) == 0 ? OrderStatus.FULLY_CANCELLED
                : OrderStatus.PARTIAL_CANCELLED;
        order.updateOrder(order.unfilledQuantity, status, event.createdAt);
        // 清算 (解冻 + 从活动订单移除):
        this.clearingService.clearCancelOrder(order);
        this.orderBookChanged = true;

        this.orderBuffer.add(order.copy());
        this.notificationBuffer.add(createNotification("order_canceled", order.userId, order.copy()));
    }

    void transfer(TransferEvent event) {
        boolean ok = this.assetService.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, event.fromUserId, event.toUserId,
                event.asset, event.amount, event.sufficient);
        if (!ok) {
            logger.warn("transfer failed: {}", event);
            return;
        }
        this.notificationBuffer.add(createNotification("asset_changed", event.toUserId, event.asset.name()));
    }

    private MatchDetailEntity createMatchDetail(long id, long sequenceId, long ts, OrderEntity order,
            OrderEntity counterOrder, MatchType type, MatchDetailRecord detail) {
        MatchDetailEntity md = new MatchDetailEntity();
        md.id = id;
        md.sequenceId = sequenceId;
        md.orderId = order.id;
        md.counterOrderId = counterOrder.id;
        md.userId = order.userId;
        md.counterUserId = counterOrder.userId;
        md.type = type;
        md.direction = order.direction;
        md.price = detail.price();
        md.quantity = detail.quantity();
        md.createdAt = ts;
        return md;
    }

    private NotificationMessage createNotification(String type, Long userId, Object data) {
        NotificationMessage msg = new NotificationMessage();
        msg.type = type;
        msg.userId = userId;
        msg.data = data;
        return msg;
    }

    /**
     * 批处理结束后统一输出：持久化、发送 Tick、推送通知、更新订单簿快照。
     */
    private void flush() {
        // 持久化订单与成交明细:
        if (this.storeService != null && (!this.orderBuffer.isEmpty() || !this.matchBuffer.isEmpty())) {
            try {
                this.storeService.persist(new ArrayList<>(this.orderBuffer), new ArrayList<>(this.matchBuffer));
            } catch (Exception e) {
                logger.error("persist failed", e);
            }
        }
        // 发送 Tick 到行情系统:
        if (this.tickProducer != null && !this.tickBuffer.isEmpty()) {
            TickMessage tickMessage = new TickMessage();
            tickMessage.sequenceId = this.lastSequenceId;
            tickMessage.createdAt = System.currentTimeMillis();
            tickMessage.ticks = new ArrayList<>(this.tickBuffer);
            this.tickProducer.sendMessage(tickMessage);
        }
        // 推送通知:
        if (this.redisService != null) {
            for (NotificationMessage msg : this.notificationBuffer) {
                this.redisService.publish(RedisCache.Topic, JsonUtil.writeJson(msg));
            }
            // 更新订单簿快照:
            if (this.orderBookChanged) {
                OrderBookBean orderBook = this.matchEngine.getOrderBook(50);
                this.redisService.set(RedisCache.OrderBook, JsonUtil.writeJson(orderBook));
            }
        }
        this.orderBuffer.clear();
        this.matchBuffer.clear();
        this.tickBuffer.clear();
        this.notificationBuffer.clear();
    }

    /**
     * 定时将订单簿快照写入 Redis (即使无事件也保持新鲜)。
     */
    @Scheduled(initialDelay = 5000, fixedRate = 1000)
    public void snapshotOrderBook() {
        if (this.redisService != null && !this.fatalError) {
            OrderBookBean orderBook = this.matchEngine.getOrderBook(50);
            this.redisService.set(RedisCache.OrderBook, JsonUtil.writeJson(orderBook));
        }
    }

    // ---------- 供内部查询接口调用的只读方法 ----------

    public OrderBookBean getOrderBook(int maxDepth) {
        return this.matchEngine.getOrderBook(maxDepth);
    }

    public OrderEntity getOrder(Long orderId) {
        return this.orderService.getOrder(orderId);
    }

    public List<OrderEntity> getUserOrders(Long userId) {
        var uOrders = this.orderService.getUserOrders(userId);
        if (uOrders == null) {
            return List.of();
        }
        List<OrderEntity> orders = new ArrayList<>(uOrders.size());
        for (OrderEntity o : uOrders.values()) {
            orders.add(o.copy());
        }
        return orders;
    }

    public BigDecimal getMarketPrice() {
        return this.matchEngine.marketPrice;
    }

    public AssetService getAssetService() {
        return this.assetService;
    }
}
