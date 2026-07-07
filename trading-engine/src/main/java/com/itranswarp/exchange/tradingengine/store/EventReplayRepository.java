package com.itranswarp.exchange.tradingengine.store;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.itranswarp.exchange.model.trade.EventEntity;

/**
 * 只读地按定序 ID 升序读取事件，供交易引擎启动时重放恢复。
 */
public interface EventReplayRepository extends CrudRepository<EventEntity, Long> {

    List<EventEntity> findAllByOrderBySequenceIdAsc();
}
