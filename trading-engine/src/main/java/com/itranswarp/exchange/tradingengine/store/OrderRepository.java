package com.itranswarp.exchange.tradingengine.store;

import org.springframework.data.repository.CrudRepository;

import com.itranswarp.exchange.model.trade.OrderEntity;

/**
 * 订单持久化仓库。
 */
public interface OrderRepository extends CrudRepository<OrderEntity, Long> {
}
