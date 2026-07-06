package com.itranswarp.exchange.tradingapi.web;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itranswarp.exchange.ApiError;
import com.itranswarp.exchange.ApiException;
import com.itranswarp.exchange.model.ui.UserProfileEntity;
import com.itranswarp.exchange.tradingapi.bean.AuthRequest;
import com.itranswarp.exchange.tradingapi.user.UserService;

/**
 * 认证接口：注册与登录。
 * <p>
 * 登录成功后返回 token (演示中即用户 ID 字符串)，客户端后续以
 * {@code Authorization: Bearer <token>} 携带。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public Map<String, Object> signup(@RequestBody AuthRequest req) {
        validate(req, true);
        UserProfileEntity profile = this.userService.signup(req.email, req.name, req.password);
        return Map.of("userId", profile.userId, "name", profile.name, "token", String.valueOf(profile.userId));
    }

    @PostMapping("/signin")
    public Map<String, Object> signin(@RequestBody AuthRequest req) {
        validate(req, false);
        UserProfileEntity profile = this.userService.signin(req.email, req.password);
        return Map.of("userId", profile.userId, "name", profile.name, "token", String.valueOf(profile.userId));
    }

    private void validate(AuthRequest req, boolean requireName) {
        if (req == null || req.email == null || req.email.isBlank() || req.password == null || req.password.isBlank()) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "Email and password are required.");
        }
        if (requireName && (req.name == null || req.name.isBlank())) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "name", "Name is required.");
        }
    }
}
