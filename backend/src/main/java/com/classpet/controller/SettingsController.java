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
    @Autowired private com.classpet.service.SchoolClassService classService;

    // Public endpoint for login page — no auth required, returns system name/theme only
    // Must be kept in sync with SecurityConfig.requestMatchers("/api/settings").permitAll()
    @GetMapping
    public ResponseEntity<?> getSettings(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of(
                    "systemName", "课堂宠物乐园",
                    "className", "",
                    "theme", "pink",
                    "themeMode", "light",
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
                    if (req.className != null && !req.className.isBlank()) {
                        // 多班级：优先改班级名，同时把教师级 className 作为"当前班级名"同步，
                        // 保证老接口 / 看板 / 标题仍然有正确显示。
                        t.setClassName(req.className);
                        if (req.classId != null && !req.classId.isBlank()) {
                            try {
                                classService.rename(req.classId, t.getId(), req.className);
                            } catch (IllegalArgumentException ignored) {
                                // 班级不属于该教师时不阻断设置保存
                            }
                        }
                    }
                    if (req.theme != null && !req.theme.isBlank()) t.setTheme(req.theme);
                    if ("dark".equals(req.themeMode) || "light".equals(req.themeMode)) t.setThemeMode(req.themeMode);
                    if (req.showOnBoard != null) t.setShowOnBoard(req.showOnBoard);
                    Teacher saved = authService.save(t);
                    return ResponseEntity.ok(toMap(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(Teacher t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("systemName", t.getSystemName());
        // 教师码：学生登录第一步要用它定位到这位老师名下的班级。
        // 目前直接复用登录用户名（老师不用记两个号），设置页可查看与复制。
        m.put("teacherCode", t.getUsername());
        m.put("className", t.getClassName());
        // 兼容字段：前端可用 /api/classes 拿完整列表
        m.put("defaultClassId", classService.list(t.getId()).stream()
                .findFirst().map(c -> c.getId()).orElse(null));
        m.put("theme", t.getTheme());
        m.put("themeMode", t.getThemeMode() == null ? "light" : t.getThemeMode());
        m.put("activated", t.isActivated());
        m.put("showOnBoard", t.isShowOnBoard());
        return m;
    }
}
