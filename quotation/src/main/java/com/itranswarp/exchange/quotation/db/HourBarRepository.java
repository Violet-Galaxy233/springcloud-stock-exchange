package com.itranswarp.exchange.quotation.db;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.itranswarp.exchange.model.quotation.HourBarEntity;

/**
 * 小时级 K 线仓库。主键为 startTime (long)。
 */
public interface HourBarRepository extends CrudRepository<HourBarEntity, Long> {

    /**
     * 按开始时间倒序取最近 100 根 K 线。
     */
    List<HourBarEntity> findTop100ByOrderByStartTimeDesc();
}
