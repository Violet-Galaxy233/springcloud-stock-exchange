package com.itranswarp.exchange.quotation.db;

import org.springframework.data.repository.CrudRepository;

import com.itranswarp.exchange.model.quotation.TickEntity;

/**
 * 成交 Tick 仓库。主键为 id (Long)。
 */
public interface TickRepository extends CrudRepository<TickEntity, Long> {
}
