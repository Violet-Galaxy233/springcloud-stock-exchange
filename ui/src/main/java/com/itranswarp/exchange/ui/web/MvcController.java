package com.itranswarp.exchange.ui.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面渲染控制器。
 * <p>
 * 将后端 API 地址与推送 WebSocket 地址注入模型, 供前端 JavaScript 直连使用。
 */
@Controller
public class MvcController {

    /** 后端交易 API 地址。 */
    @Value("${exchange.api-endpoint}")
    String apiEndpoint;

    /** 推送服务 WebSocket 地址 (浏览器直连)。 */
    @Value("${exchange.push-endpoint}")
    String pushEndpoint;

    /**
     * 渲染单页交易界面。
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("apiEndpoint", apiEndpoint);
        model.addAttribute("pushEndpoint", pushEndpoint);
        return "index";
    }
}
