package com.itranswarp.exchange.tradingsequencer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 事件持久化仓库。
 */
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    /**
     * 返回当前最大的定序 ID，无数据返回 0 (用于启动时恢复计数器)。
     */
    @Query("select coalesce(max(e.sequenceId), 0) from EventEntity e")
    long getMaxSequenceId();
}
