package com.itranswarp.exchange.model.ui;

import com.itranswarp.exchange.model.support.EntitySupport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 口令认证信息 (与 UserEntity 一对一，userId 为主键)。
 * <p>
 * passwd 存储 SHA-256 摘要，仅为演示，生产环境应使用加盐的强哈希。
 */
@Entity
@Table(name = "password_auths")
public class PasswordAuthEntity implements EntitySupport {

    @Id
    @Column(nullable = false, updatable = false)
    public Long userId;

    @Column(nullable = false, length = 100)
    public String random;

    @Column(nullable = false, length = 64)
    public String passwd;

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
