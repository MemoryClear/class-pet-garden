package com.classpet.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * SPA routing forward controller.
 * Forwards Vue Router paths to index.html to avoid 404 on page refresh.
 */
@Controller
@RequestMapping("/")
public class SpaController {

    @GetMapping(value = {
        "/", "/home", "/activate", "/history",
        "/exchange-history", "/leaderboard", "/shop", "/settings"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}