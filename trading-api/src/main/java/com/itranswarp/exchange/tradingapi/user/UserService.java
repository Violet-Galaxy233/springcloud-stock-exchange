package com.itranswarp.exchange.tradingapi.user;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.exchange.ApiError;
import com.itranswarp.exchange.ApiException;
import com.itranswarp.exchange.enums.UserType;
import com.itranswarp.exchange.model.ui.PasswordAuthEntity;
import com.itranswarp.exchange.model.ui.UserEntity;
import com.itranswarp.exchange.model.ui.UserProfileEntity;
import com.itranswarp.exchange.support.LoggerSupport;
import com.itranswarp.exchange.util.HashUtil;

/**
 * 用户注册与登录服务。
 * <p>
 * 口令以 SHA-256 摘要存储，仅为演示；生产环境应使用加盐的强哈希 (如 BCrypt)。
 */
@Component
public class UserService extends LoggerSupport {

    final UserRepository userRepository;
    final UserProfileRepository userProfileRepository;
    final PasswordAuthRepository passwordAuthRepository;

    public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository,
            PasswordAuthRepository passwordAuthRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordAuthRepository = passwordAuthRepository;
    }

    @Transactional
    public UserProfileEntity signup(String email, String name, String password) {
        final long ts = System.currentTimeMillis();
        email = email.strip().toLowerCase();
        if (this.userProfileRepository.findByEmail(email).isPresent()) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "email", "Email already exists.");
        }
        // 创建用户:
        UserEntity user = new UserEntity();
        user.type = UserType.TRADER;
        user.createdAt = ts;
        this.userRepository.save(user);
        // 创建用户资料:
        UserProfileEntity profile = new UserProfileEntity();
        profile.userId = user.id;
        profile.email = email;
        profile.name = name;
        profile.createdAt = profile.updatedAt = ts;
        this.userProfileRepository.save(profile);
        // 创建口令认证:
        PasswordAuthEntity auth = new PasswordAuthEntity();
        auth.userId = user.id;
        auth.random = Long.toHexString(ts);
        auth.passwd = HashUtil.sha256(password);
        auth.createdAt = ts;
        this.passwordAuthRepository.save(auth);
        logger.info("signup ok: userId={}, email={}", user.id, email);
        return profile;
    }

    public UserProfileEntity signin(String email, String password) {
        email = email.strip().toLowerCase();
        UserProfileEntity profile = this.userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ApiError.AUTH_SIGNIN_FAILED, "Invalid email or password."));
        PasswordAuthEntity auth = this.passwordAuthRepository.findById(profile.userId)
                .orElseThrow(() -> new ApiException(ApiError.AUTH_SIGNIN_FAILED, "Invalid email or password."));
        if (!auth.passwd.equals(HashUtil.sha256(password))) {
            throw new ApiException(ApiError.AUTH_SIGNIN_FAILED, "Invalid email or password.");
        }
        return profile;
    }

    public UserProfileEntity getProfile(Long userId) {
        return this.userProfileRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ApiError.NOT_FOUND, "User not found."));
    }
}
