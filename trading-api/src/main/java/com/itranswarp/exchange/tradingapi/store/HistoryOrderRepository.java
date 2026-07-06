package com.itranswarp.exchange.tradingapi.store;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itranswarp.exchange.model.trade.OrderEntity;

/**
 * 历史订单查询 (读取交易引擎持久化的订单表)。
 */
public interface HistoryOrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findTop100ByUserIdOrderByIdDesc(Long userId);
}
