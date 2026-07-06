package com.itranswarp.exchange.model.support;

/**
 * 所有数据库实体的公共接口，约定含有创建时间与更新时间。
 */
public interface EntitySupport {

    long getCreatedAt();

    long getUpdatedAt();
}
