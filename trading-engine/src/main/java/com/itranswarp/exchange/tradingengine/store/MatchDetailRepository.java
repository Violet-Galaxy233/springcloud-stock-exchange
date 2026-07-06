package com.itranswarp.exchange.tradingengine.store;

import org.springframework.data.repository.CrudRepository;

import com.itranswarp.exchange.model.trade.MatchDetailEntity;

/**
 * 成交明细持久化仓库。
 */
public interface MatchDetailRepository extends CrudRepository<MatchDetailEntity, Long> {
}
