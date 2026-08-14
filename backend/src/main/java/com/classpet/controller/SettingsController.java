package com.classpet.controller;

import com.classpet.dto.SettingsDto;
import com.classpet.entity.Teacher;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.classpet.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired private AuthService authService;

    // Public endpoint for login page — no auth required, returns system name/theme only
    // Must be kept in sync with SecurityConfig.requestMatchers("/api/settings").permitAll()
    @GetMapping
    public ResponseEntity<?> getSettings(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of(
                    "systemName", "课堂宠物乐园",
                    "className", "",
                    "theme", "pink",
                    "publicOnly", true
            ));
        }
        return authService.findById(principal.teacherId())
                .map(t -> ResponseEntity.ok(toMap(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody SettingsDto.UpdateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        return authService.findById(principal.teacherId())
                .map(t -> {
                    if (req.systemName != null && !req.systemName.isBlank()) t.setSystemName(req.systemName);
                    if (req.className != null && !req.className.isBlank()) t.setClassName(req.className);
                    if (req.theme != null && !req.theme.isBlank()) t.setTheme(req.theme);
                    if (req.showOnBoard != null) t.setShowOnBoard(req.showOnBoard);
                    Teacher saved = authService.save(t);
                    return ResponseEntity.ok(toMap(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(Teacher t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("systemName", t.getSystemName());
        m.put("className", t.getClassName());
        m.put("theme", t.getTheme());
        m.put("activated", t.isActivated());
        m.put("showOnBoard", t.isShowOnBoard());
        return m;
    }
}
