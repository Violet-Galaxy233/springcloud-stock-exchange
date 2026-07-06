package com.itranswarp.exchange.tradingengine.store;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.exchange.model.trade.MatchDetailEntity;
import com.itranswarp.exchange.model.trade.OrderEntity;
import com.itranswarp.exchange.support.LoggerSupport;

/**
 * 持久化服务：将订单与成交明细批量写入数据库。
 * <p>
 * 交易引擎全内存运行，持久化在事件处理主流程之外异步/批量进行，不影响撮合性能。
 */
@Component
public class StoreService extends LoggerSupport {

    final OrderRepository orderRepository;
    final MatchDetailRepository matchDetailRepository;

    public StoreService(OrderRepository orderRepository, MatchDetailRepository matchDetailRepository) {
        this.orderRepository = orderRepository;
        this.matchDetailRepository = matchDetailRepository;
    }

    @Transactional
    public void persist(List<OrderEntity> orders, List<MatchDetailEntity> matchDetails) {
        if (orders != null && !orders.isEmpty()) {
            this.orderRepository.saveAll(orders);
        }
        if (matchDetails != null && !matchDetails.isEmpty()) {
            this.matchDetailRepository.saveAll(matchDetails);
        }
    }
}
