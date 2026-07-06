package com.itranswarp.exchange.tradingapi.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itranswarp.exchange.model.ui.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
