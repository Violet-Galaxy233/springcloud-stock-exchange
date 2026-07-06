package com.itranswarp.exchange.quotation.db;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.itranswarp.exchange.model.quotation.MinBarEntity;

/**
 * 分钟级 K 线仓库。主键为 startTime (long)。
 */
public interface MinBarRepository extends CrudRepository<MinBarEntity, Long> {

    /**
     * 按开始时间倒序取最近 100 根 K 线。
     */
    List<MinBarEntity> findTop100ByOrderByStartTimeDesc();
}
