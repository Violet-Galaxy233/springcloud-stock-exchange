package com.itranswarp.exchange.model.ui;

import com.itranswarp.exchange.model.support.EntitySupport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 用户资料 (与 UserEntity 一对一，userId 为主键)。
 */
@Entity
@Table(name = "user_profiles")
public class UserProfileEntity implements EntitySupport {

    @Id
    @Column(nullable = false, updatable = false)
    public Long userId;

    @Column(nullable = false, length = 100)
    public String email;

    @Column(nullable = false, length = 100)
    public String name;

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
}
