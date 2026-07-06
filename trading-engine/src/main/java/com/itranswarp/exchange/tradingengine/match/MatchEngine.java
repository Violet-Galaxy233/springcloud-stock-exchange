package com.itranswarp.exchange.tradingengine.match;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.itranswarp.exchange.bean.OrderBookBean;
import com.itranswarp.exchange.bean.OrderBookItemBean;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.OrderStatus;
import com.itranswarp.exchange.model.trade.OrderEntity;

/**
 * 撮合引擎。
 * <p>
 * 维护买盘 {@link #buyBook} 与卖盘 {@link #sellBook} 两个订单簿。
 * 处理一个 taker 订单时，不断与对手盘最优订单撮合，成交则生成成交明细，
 * 撮合不尽的剩余部分放入自己的订单簿等待后续成交。
 * <p>
 * 撮合以 <b>maker 挂单价</b> 成交，遵循"价格优先、时间优先"原则。
 */
@Component
public class MatchEngine {

    public final OrderBook buyBook = new OrderBook(Direction.BUY);
    public final OrderBook sellBook = new OrderBook(Direction.SELL);

    /**
     * 最新市场价 (最近一次成交价)。
     */
    public BigDecimal marketPrice = BigDecimal.ZERO;

    /**
     * 上次处理的定序 ID。
     */
    private long sequenceId;

    /**
     * 处理一个订单，返回撮合结果。
     */
    public MatchResult processOrder(long sequenceId, OrderEntity order) {
        return switch (order.direction) {
            // 买入，与卖盘撮合，剩余放入买盘:
            case BUY -> processOrder(sequenceId, order, this.sellBook, this.buyBook);
            // 卖出，与买盘撮合，剩余放入卖盘:
            case SELL -> processOrder(sequenceId, order, this.buyBook, this.sellBook);
            default -> throw new IllegalArgumentException("Invalid direction.");
        };
    }

    private MatchResult processOrder(long sequenceId, OrderEntity takerOrder, OrderBook makerBook, OrderBook anotherBook) {
        this.sequenceId = sequenceId;
        long ts = takerOrder.createdAt;
        MatchResult matchResult = new MatchResult(takerOrder);
        BigDecimal takerUnfilledQuantity = takerOrder.quantity;
        for (;;) {
            OrderEntity makerOrder = makerBook.getFirst();
            if (makerOrder == null) {
                // 对手盘空，无法继续撮合:
                break;
            }
            if (takerOrder.direction == Direction.BUY && takerOrder.price.compareTo(makerOrder.price) < 0) {
                // 买入价格低于最优卖价，无法成交:
                break;
            } else if (takerOrder.direction == Direction.SELL && takerOrder.price.compareTo(makerOrder.price) > 0) {
                // 卖出价格高于最优买价，无法成交:
                break;
            }
            // 以 maker 订单价格成交:
            this.marketPrice = makerOrder.price;
            // 本次成交数量为两者未成交量的较小值:
            BigDecimal matchedQuantity = takerUnfilledQuantity.min(makerOrder.unfilledQuantity);
            // 记录成交明细:
            matchResult.add(makerOrder.price, matchedQuantity, makerOrder);
            // 更新 taker 与 maker 的未成交量:
            takerUnfilledQuantity = takerUnfilledQuantity.subtract(matchedQuantity);
            BigDecimal makerUnfilledQuantity = makerOrder.unfilledQuantity.subtract(matchedQuantity);
            if (makerUnfilledQuantity.signum() == 0) {
                // maker 完全成交后，从订单簿中删除:
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatus.FULLY_FILLED, ts);
                makerBook.remove(makerOrder);
            } else {
                // maker 部分成交:
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatus.PARTIAL_FILLED, ts);
            }
            // taker 完全成交后，退出循环:
            if (takerUnfilledQuantity.signum() == 0) {
                takerOrder.updateOrder(takerUnfilledQuantity, OrderStatus.FULLY_FILLED, ts);
                break;
            }
        }
        // taker 未完全成交时，放入对应订单簿:
        if (takerUnfilledQuantity.signum() > 0) {
            takerOrder.updateOrder(takerUnfilledQuantity,
                    takerUnfilledQuantity.compareTo(takerOrder.quantity) == 0 ? OrderStatus.PENDING
                            : OrderStatus.PARTIAL_FILLED,
                    ts);
            anotherBook.add(takerOrder);
        }
        return matchResult;
    }

    /**
     * 撤单：从对应订单簿中移除。
     */
    public void remove(OrderEntity order) {
        OrderBook book = order.direction == Direction.BUY ? this.buyBook : this.sellBook;
        if (!book.remove(order)) {
            throw new IllegalArgumentException("Order not found in order book: " + order);
        }
    }

    /**
     * 生成订单簿快照，买卖双方各取前 maxDepth 档 (按价格聚合数量)。
     */
    public OrderBookBean getOrderBook(int maxDepth) {
        return new OrderBookBean(this.sequenceId, this.marketPrice, getOrderBook(this.buyBook, maxDepth),
                getOrderBook(this.sellBook, maxDepth));
    }

    private List<OrderBookItemBean> getOrderBook(OrderBook orderBook, int maxDepth) {
        List<OrderBookItemBean> items = new ArrayList<>(maxDepth);
        OrderBookItemBean prevItem = null;
        for (Map.Entry<OrderKey, OrderEntity> entry : orderBook.book.entrySet()) {
            OrderEntity order = entry.getValue();
            if (prevItem == null || prevItem.price.compareTo(order.price) != 0) {
                if (items.size() >= maxDepth) {
                    break;
                }
                prevItem = new OrderBookItemBean(order.price, order.unfilledQuantity);
                items.add(prevItem);
            } else {
                // 相同价格，累加数量:
                prevItem.addQuantity(order.unfilledQuantity);
            }
        }
        return items;
    }

    public void debug() {
        System.out.println("---------- match engine ----------");
        System.out.println("  SELL:\n" + this.sellBook);
        System.out.println("  market price: " + this.marketPrice);
        System.out.println("  BUY:\n" + this.buyBook);
        System.out.println("----------------------------------");
    }
}
