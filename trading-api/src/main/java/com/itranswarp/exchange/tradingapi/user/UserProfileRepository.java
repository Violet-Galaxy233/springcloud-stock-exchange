package com.itranswarp.exchange.tradingapi.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itranswarp.exchange.model.ui.UserProfileEntity;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

    Optional<UserProfileEntity> findByEmail(String email);
}
