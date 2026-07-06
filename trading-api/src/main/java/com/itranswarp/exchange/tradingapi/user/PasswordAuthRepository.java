package com.itranswarp.exchange.tradingapi.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itranswarp.exchange.model.ui.PasswordAuthEntity;

public interface PasswordAuthRepository extends JpaRepository<PasswordAuthEntity, Long> {
}
