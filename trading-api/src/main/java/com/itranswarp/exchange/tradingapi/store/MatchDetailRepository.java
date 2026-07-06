package com.itranswarp.exchange.tradingapi.store;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itranswarp.exchange.model.trade.MatchDetailEntity;

/**
 * 历史成交明细查询。
 */
public interface MatchDetailRepository extends JpaRepository<MatchDetailEntity, Long> {

    List<MatchDetailEntity> findByOrderIdOrderByIdAsc(Long orderId);

    List<MatchDetailEntity> findTop100ByUserIdOrderByIdDesc(Long userId);
}
