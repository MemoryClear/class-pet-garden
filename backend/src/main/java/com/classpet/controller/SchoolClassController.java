package com.classpet.controller;

import com.classpet.entity.SchoolClass;
import com.classpet.repository.StudentRepository;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.classpet.service.SchoolClassService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 班级管理（教师端，需要登录）。
 *
 * <p>学生登录页用的公开班级列表在 {@link AuthController#classes()}（/api/auth/classes）。</p>
 */
@RestController
@RequestMapping("/api/classes")
public class SchoolClassController {

    private final SchoolClassService classService;
    private final StudentRepository studentRepository;

    public SchoolClassController(SchoolClassService classService, StudentRepository studentRepository) {
        this.classService = classService;
        this.studentRepository = studentRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        List<SchoolClass> classes = classService.list(principal.teacherId());
        // 老账号第一次进来可能还没有班级，这里顺手补一个
        if (classes.isEmpty()) {
            classService.resolve(null, principal.teacherId());
            classes = classService.list(principal.teacherId());
        }
        List<Map<String, Object>> out = new ArrayList<>(classes.size());
        for (SchoolClass c : classes) {
            out.add(toMap(c));
        }
        return ResponseEntity.ok(out);
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @RequestBody Map<String, String> body) {
        try {
            SchoolClass c = classService.create(principal.teacherId(), body.get("name"));
            return ResponseEntity.ok(toMap(c));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> rename(@PathVariable String id,
                                    @AuthenticationPrincipal AuthenticatedUser principal,
                                    @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(toMap(classService.rename(id, principal.teacherId(), body.get("name"))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/board")
    public ResponseEntity<?> setBoard(@PathVariable String id,
                                      @AuthenticationPrincipal AuthenticatedUser principal,
                                      @RequestBody Map<String, Object> body) {
        boolean show = Boolean.TRUE.equals(body.get("showOnBoard"));
        try {
            return ResponseEntity.ok(toMap(classService.setShowOnBoard(id, principal.teacherId(), show)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        try {
            classService.delete(id, principal.teacherId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(SchoolClass c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("code", c.getCode());
        m.put("showOnBoard", Boolean.TRUE.equals(c.getShowOnBoard()));
        m.put("sortOrder", c.getSortOrder());
        m.put("studentCount", studentRepository.countByClassId(c.getId()));
        return m;
    }
}
