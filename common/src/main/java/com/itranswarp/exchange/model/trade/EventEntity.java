package com.itranswarp.exchange.model.trade;

import com.itranswarp.exchange.model.support.EntitySupport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 已定序事件的持久化记录。
 * <p>
 * 定序服务写入，交易引擎在启动时可据此按序重放以重建内存状态 (故障恢复)。
 */
@Entity
@Table(name = "events")
public class EventEntity implements EntitySupport {

    /**
     * 定序 ID，作为主键，全局唯一且递增。
     */
    @Id
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * 上一个事件的定序 ID。
     */
    @Column(nullable = false, updatable = false)
    public long previousId;

    /**
     * 事件内容 (JSON，含 @class 多态类型信息)。
     */
    @Column(nullable = false, updatable = false, length = 10000)
    public String data;

    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Override
    public long getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public long getUpdatedAt() {
        return this.createdAt;
    }
}
