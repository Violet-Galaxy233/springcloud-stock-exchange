package com.itranswarp.exchange.tradingengine.match;

import java.util.Comparator;
import java.util.TreeMap;

import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.model.trade.OrderEntity;

/**
 * 单方向的订单簿 (买盘或卖盘)。
 * <p>
 * 使用 {@link TreeMap} 按"价格优先、时间优先"排序：
 * <ul>
 * <li>买盘：价格从高到低，价格相同则定序 ID 从小到大 (先到先得)；</li>
 * <li>卖盘：价格从低到高，价格相同则定序 ID 从小到大。</li>
 * </ul>
 * 这样订单簿的第一个元素永远是"最优可成交"的订单。
 */
public class OrderBook {

    /**
     * 卖盘排序：价格升序，价格相同时定序 ID 升序。
     */
    private static final Comparator<OrderKey> SORT_SELL = Comparator.comparing(OrderKey::price)
            .thenComparingLong(OrderKey::sequenceId);

    /**
     * 买盘排序：价格降序，价格相同时定序 ID 升序。
     */
    private static final Comparator<OrderKey> SORT_BUY = Comparator.comparing(OrderKey::price).reversed()
            .thenComparingLong(OrderKey::sequenceId);

    public final Direction direction;
    public final TreeMap<OrderKey, OrderEntity> book;

    public OrderBook(Direction direction) {
        this.direction = direction;
        this.book = new TreeMap<>(direction == Direction.BUY ? SORT_BUY : SORT_SELL);
    }

    /**
     * 返回最优 (第一个) 订单，无订单返回 null。
     */
    public OrderEntity getFirst() {
        return this.book.isEmpty() ? null : this.book.firstEntry().getValue();
    }

    public boolean remove(OrderEntity order) {
        return this.book.remove(new OrderKey(order.sequenceId, order.price)) != null;
    }

    public boolean add(OrderEntity order) {
        return this.book.put(new OrderKey(order.sequenceId, order.price), order) == null;
    }

    public boolean exist(OrderEntity order) {
        return this.book.containsKey(new OrderKey(order.sequenceId, order.price));
    }

    public int size() {
        return this.book.size();
    }

    @Override
    public String toString() {
        if (this.book.isEmpty()) {
            return "(empty)";
        }
        StringBuilder sb = new StringBuilder();
        for (OrderEntity order : this.book.descendingMap().values()) {
            sb.append("  ").append(order.price).append(" ").append(order.unfilledQuantity).append("\n");
        }
        return sb.toString();
    }
}
