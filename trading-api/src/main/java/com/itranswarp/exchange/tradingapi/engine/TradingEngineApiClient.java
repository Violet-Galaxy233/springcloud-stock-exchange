package com.itranswarp.exchange.tradingapi.engine;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.itranswarp.exchange.bean.OrderBookBean;
import com.itranswarp.exchange.model.trade.OrderEntity;
import com.itranswarp.exchange.support.LoggerSupport;

/**
 * 交易引擎内部查询接口的 HTTP 客户端 (架构图中 API -> Engine 的 query 通道)。
 * <p>
 * 下单/撤单走 Kafka 异步链路，而实时查询 (订单簿、活动订单、资产) 直接同步调用交易引擎。
 */
@Component
public class TradingEngineApiClient extends LoggerSupport {

    private final RestClient restClient;

    public TradingEngineApiClient(@Value("${exchange.engine-api-endpoint:http://localhost:8002}") String endpoint) {
        this.restClient = RestClient.builder().baseUrl(endpoint).build();
        logger.info("trading engine query endpoint: {}", endpoint);
    }

    public OrderBookBean getOrderBook() {
        return this.restClient.get().uri("/internal/orderBook").retrieve().body(OrderBookBean.class);
    }

    public Map<String, Object> getMarketPrice() {
        return this.restClient.get().uri("/internal/marketPrice").retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }

    public OrderEntity getOrder(Long orderId) {
        return this.restClient.get().uri("/internal/order/{orderId}", orderId).retrieve().body(OrderEntity.class);
    }

    public List<OrderEntity> getUserOrders(Long userId) {
        return this.restClient.get().uri("/internal/orders/{userId}", userId).retrieve()
                .body(new ParameterizedTypeReference<List<OrderEntity>>() {
                });
    }

    public Map<String, Object> getUserAssets(Long userId) {
        return this.restClient.get().uri("/internal/assets/{userId}", userId).retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }
}
