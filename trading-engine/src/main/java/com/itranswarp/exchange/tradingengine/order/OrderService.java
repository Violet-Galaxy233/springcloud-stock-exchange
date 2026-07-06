package com.itranswarp.exchange.tradingengine.order;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.OrderStatus;
import com.itranswarp.exchange.model.trade.OrderEntity;
import com.itranswarp.exchange.support.LoggerSupport;
import com.itranswarp.exchange.tradingengine.assets.AssetService;

/**
 * 订单系统：管理所有活动订单 (尚未完全成交且未取消的订单)，并在创建订单时冻结相应资产。
 */
@Component
public class OrderService extends LoggerSupport {

    // 引用 AssetService:
    final AssetService assetService;

    // 跟踪所有活动订单: Order ID => OrderEntity
    final ConcurrentMap<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();

    // 跟踪用户活动订单: User ID => (Order ID => OrderEntity)
    final ConcurrentMap<Long, ConcurrentMap<Long, OrderEntity>> userOrders = new ConcurrentHashMap<>();

    public OrderService(AssetService assetService) {
        this.assetService = assetService;
    }

    /**
     * 创建订单，成功返回订单，资产冻结失败返回 null。
     */
    public OrderEntity createOrder(long sequenceId, long ts, Long orderId, Long userId, Direction direction,
            BigDecimal price, BigDecimal quantity) {
        switch (direction) {
            case BUY -> {
                // 买入，需冻结 USD = 价格 * 数量:
                if (!this.assetService.tryFreeze(userId, AssetEnum.USD, price.multiply(quantity))) {
                    return null;
                }
            }
            case SELL -> {
                // 卖出，需冻结 BTC = 数量:
                if (!this.assetService.tryFreeze(userId, AssetEnum.BTC, quantity)) {
                    return null;
                }
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }
        // 实例化 Order:
        OrderEntity order = new OrderEntity();
        order.id = orderId;
        order.sequenceId = sequenceId;
        order.userId = userId;
        order.direction = direction;
        order.price = price;
        order.quantity = quantity;
        order.unfilledQuantity = quantity;
        order.status = OrderStatus.PENDING;
        order.createdAt = order.updatedAt = ts;
        // 添加到活动订单:
        this.activeOrders.put(order.id, order);
        // 添加到用户活动订单:
        ConcurrentMap<Long, OrderEntity> uOrders = this.userOrders.computeIfAbsent(userId,
                k -> new ConcurrentHashMap<>());
        uOrders.put(order.id, order);
        return order;
    }

    public OrderEntity getOrder(Long orderId) {
        return this.activeOrders.get(orderId);
    }

    public ConcurrentMap<Long, OrderEntity> getActiveOrders() {
        return this.activeOrders;
    }

    public ConcurrentMap<Long, OrderEntity> getUserOrders(Long userId) {
        return this.userOrders.get(userId);
    }

    /**
     * 删除一个活动订单 (完全成交或已取消)，同时从 activeOrders 与 userOrders 中移除。
     */
    public void removeOrder(Long orderId) {
        OrderEntity removed = this.activeOrders.remove(orderId);
        if (removed == null) {
            throw new IllegalArgumentException("Order not found by orderId in active orders: " + orderId);
        }
        ConcurrentMap<Long, OrderEntity> uOrders = this.userOrders.get(removed.userId);
        if (uOrders == null) {
            throw new IllegalArgumentException("User orders not found by userId: " + removed.userId);
        }
        if (uOrders.remove(orderId) == null) {
            throw new IllegalArgumentException("Order not found by orderId in user orders: " + orderId);
        }
    }

    public void debug() {
        System.out.println("---------- active orders ----------");
        for (Map.Entry<Long, OrderEntity> entry : this.activeOrders.entrySet()) {
            System.out.println("  " + entry.getValue());
        }
        System.out.println("-----------------------------------");
    }
}
