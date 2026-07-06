package com.itranswarp.exchange.tradingengine.clearing;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.model.trade.OrderEntity;
import com.itranswarp.exchange.support.LoggerSupport;
import com.itranswarp.exchange.tradingengine.assets.AssetService;
import com.itranswarp.exchange.tradingengine.assets.Transfer;
import com.itranswarp.exchange.tradingengine.match.MatchDetailRecord;
import com.itranswarp.exchange.tradingengine.match.MatchResult;
import com.itranswarp.exchange.tradingengine.order.OrderService;

/**
 * 清算系统：处理来自撮合引擎的撮合结果，完成买卖双方的资产交换。
 */
@Component
public class ClearingService extends LoggerSupport {

    final AssetService assetService;
    final OrderService orderService;

    public ClearingService(AssetService assetService, OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    /**
     * 清算撮合结果：逐条成交明细完成买卖双方资产交换。以 maker 挂单价成交，
     * 若 taker (买方) 挂单价高于成交价，多冻结的差额退回 taker。
     */
    public void clearMatchResult(MatchResult result) {
        OrderEntity taker = result.takerOrder;
        switch (taker.direction) {
            case BUY -> {
                // taker 买入，maker 卖出:
                for (MatchDetailRecord detail : result.matchDetails) {
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal matched = detail.quantity();
                    // 如果 maker 价格 < taker 价格，则 taker 少花钱，退回差额:
                    if (taker.price.compareTo(maker.price) > 0) {
                        BigDecimal unfreezeQuote = taker.price.subtract(maker.price).multiply(matched);
                        this.assetService.unfreeze(taker.userId, AssetEnum.USD, unfreezeQuote);
                    }
                    // taker 用冻结的 USD 支付给 maker (按成交价):
                    this.assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.USD,
                            maker.price.multiply(matched));
                    // maker 用冻结的 BTC 交付给 taker:
                    this.assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.BTC,
                            matched);
                    // maker 完全成交后，从活动订单中删除:
                    if (maker.unfilledQuantity.signum() == 0) {
                        this.orderService.removeOrder(maker.id);
                    }
                }
                // taker 完全成交后，从活动订单中删除:
                if (taker.unfilledQuantity.signum() == 0) {
                    this.orderService.removeOrder(taker.id);
                }
            }
            case SELL -> {
                // taker 卖出，maker 买入:
                for (MatchDetailRecord detail : result.matchDetails) {
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal matched = detail.quantity();
                    // taker 用冻结的 BTC 交付给 maker:
                    this.assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.BTC,
                            matched);
                    // maker 用冻结的 USD 支付给 taker (按成交价即 maker 挂单价):
                    this.assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.USD,
                            maker.price.multiply(matched));
                    if (maker.unfilledQuantity.signum() == 0) {
                        this.orderService.removeOrder(maker.id);
                    }
                }
                if (taker.unfilledQuantity.signum() == 0) {
                    this.orderService.removeOrder(taker.id);
                }
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }
    }

    /**
     * 清算撤单：解冻订单剩余未成交部分对应的资产，并从活动订单中删除。
     */
    public void clearCancelOrder(OrderEntity order) {
        switch (order.direction) {
            case BUY ->
                // 解冻剩余 USD = 价格 * 未成交数量:
                this.assetService.unfreeze(order.userId, AssetEnum.USD,
                        order.price.multiply(order.unfilledQuantity));
            case SELL ->
                // 解冻剩余 BTC = 未成交数量:
                this.assetService.unfreeze(order.userId, AssetEnum.BTC, order.unfilledQuantity);
            default -> throw new IllegalArgumentException("Invalid direction.");
        }
        // 从活动订单中删除:
        this.orderService.removeOrder(order.id);
    }
}
