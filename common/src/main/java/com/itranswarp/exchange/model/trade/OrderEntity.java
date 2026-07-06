package com.itranswarp.exchange.model.trade;

import java.math.BigDecimal;
import java.util.Objects;

import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.OrderStatus;
import com.itranswarp.exchange.model.support.EntitySupport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * 订单实体。
 * <p>
 * 出于简化设计，该对象既作为交易引擎内存中的订单对象，也作为数据库映射实体。
 * <p>
 * 订单创建成功后，后续由撮合引擎处理时，只有 {@link #unfilledQuantity} 与
 * {@link #status} 会发生变化，其余属性均只读。
 */
@Entity
@Table(name = "orders")
public class OrderEntity implements EntitySupport, Comparable<OrderEntity> {

    /**
     * 订单 ID。
     */
    @Id
    @Column(nullable = false, updatable = false)
    public Long id;

    /**
     * 定序 ID，相同价格的订单根据定序 ID 排序 (时间优先)。
     */
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * 订单关联的用户 ID。
     */
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * 订单价格。
     */
    @Column(nullable = false, updatable = false, precision = 36, scale = 18)
    public BigDecimal price;

    /**
     * 订单方向。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    public Direction direction;

    /**
     * 订单状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public OrderStatus status;

    /**
     * 订单数量。
     */
    @Column(nullable = false, updatable = false, precision = 36, scale = 18)
    public BigDecimal quantity;

    /**
     * 尚未成交的数量。
     */
    @Column(nullable = false, precision = 36, scale = 18)
    public BigDecimal unfilledQuantity;

    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Column(nullable = false)
    public long updatedAt;

    @Override
    public long getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public long getUpdatedAt() {
        return this.updatedAt;
    }

    /**
     * 更新订单状态与未成交数量 (由撮合/清算引擎调用)。
     */
    public void updateOrder(BigDecimal unfilledQuantity, OrderStatus status, long updatedAt) {
        this.unfilledQuantity = unfilledQuantity;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    /**
     * 返回一份不可变的副本，用于对外暴露，避免外部修改内部状态。
     */
    @Transient
    public OrderEntity copy() {
        OrderEntity entity = new OrderEntity();
        entity.id = this.id;
        entity.sequenceId = this.sequenceId;
        entity.userId = this.userId;
        entity.price = this.price;
        entity.direction = this.direction;
        entity.status = this.status;
        entity.quantity = this.quantity;
        entity.unfilledQuantity = this.unfilledQuantity;
        entity.createdAt = this.createdAt;
        entity.updatedAt = this.updatedAt;
        return entity;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof OrderEntity e) {
            return Objects.equals(this.id, e.id);
        }
        return false;
    }

    /**
     * 按定序 ID 升序 (时间优先)。
     */
    @Override
    public int compareTo(OrderEntity o) {
        return Long.compare(this.sequenceId, o.sequenceId);
    }

    @Override
    public String toString() {
        return "OrderEntity [id=" + id + ", sequenceId=" + sequenceId + ", userId=" + userId + ", price=" + price
                + ", direction=" + direction + ", status=" + status + ", quantity=" + quantity + ", unfilledQuantity="
                + unfilledQuantity + "]";
    }
}
