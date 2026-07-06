package com.itranswarp.exchange.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户 Web 界面服务入口。
 * <p>
 * 仅负责渲染单页交易界面, 页面内 JavaScript 直连后端 trading-api 与 push WebSocket。
 */
@SpringBootApplication
public class UIApplication {

    public static void main(String[] args) {
        SpringApplication.run(UIApplication.class, args);
    }
}
