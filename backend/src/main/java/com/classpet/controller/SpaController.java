package com.classpet.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA 路由转发控制器
 * 将 Vue Router 路由转发到 index.html，避免刷新页面时 404
 */
@Controller
public class SpaController {

    // Vue Router 路径转发到 index.html
    @GetMapping(value = {
        "/",
        "/home",
        "/home/**",
        "/activate",
        "/activate/**",
        "/history",
        "/history/**",
        "/exchange-history",
        "/exchange-history/**",
        "/leaderboard",
        "/leaderboard/**",
        "/shop",
        "/shop/**",
        "/settings",
        "/settings/**"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}