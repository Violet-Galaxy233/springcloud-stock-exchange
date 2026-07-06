package com.itranswarp.exchange.tradingengine.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itranswarp.exchange.bean.OrderBookBean;
import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.model.trade.OrderEntity;
import com.itranswarp.exchange.tradingengine.TradingEngineService;
import com.itranswarp.exchange.tradingengine.assets.Asset;

/**
 * 交易引擎的内部查询接口 (架构图中 API -> Engine 的 query 通道)。
 * <p>
 * 仅供其他后端服务调用，不直接对外暴露。
 */
@RestController
@RequestMapping("/internal")
public class TradingEngineController {

    final TradingEngineService tradingEngineService;

    public TradingEngineController(TradingEngineService tradingEngineService) {
        this.tradingEngineService = tradingEngineService;
    }

    @GetMapping("/orderBook")
    public OrderBookBean getOrderBook() {
        return this.tradingEngineService.getOrderBook(50);
    }

    @GetMapping("/marketPrice")
    public Map<String, BigDecimal> getMarketPrice() {
        return Map.of("price", this.tradingEngineService.getMarketPrice());
    }

    @GetMapping("/order/{orderId}")
    public OrderEntity getOrder(@PathVariable("orderId") Long orderId) {
        OrderEntity order = this.tradingEngineService.getOrder(orderId);
        return order == null ? null : order.copy();
    }

    @GetMapping("/orders/{userId}")
    public List<OrderEntity> getUserOrders(@PathVariable("userId") Long userId) {
        return this.tradingEngineService.getUserOrders(userId);
    }

    @GetMapping("/assets/{userId}")
    public Map<AssetEnum, Asset> getUserAssets(@PathVariable("userId") Long userId) {
        return this.tradingEngineService.getAssetService().getAssets(userId);
    }
}
