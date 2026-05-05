package com.classpet.controller;

import com.classpet.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.classpet.service.ClassroomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

    @Autowired
    private ClassroomService classroomService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getClassrooms(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        // 确保默认教室已初始化
        classroomService.initializeDefaults(principal.teacherId());
        List<com.classpet.entity.Classroom> list = classroomService.getClassrooms(principal.teacherId());
        return ResponseEntity.ok(list.stream().map(this::toMap).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getClassroom(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String id) {
        return classroomService.getClassrooms(principal.teacherId()).stream()
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .map(c -> ResponseEntity.ok(toMap(c)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createClassroom(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            String type = (String) body.get("type");
            String config = body.get("config") != null ? body.get("config").toString() : "{}";
            if (name == null || type == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "name和type必填"));
            }
            var c = classroomService.createClassroom(principal.teacherId(), name, type, config);
            return ResponseEntity.ok(toMap(c));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateClassroom(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            String config = body.get("config") != null ? body.get("config").toString() : null;
            var c = classroomService.updateClassroom(principal.teacherId(), id, name, config);
            return ResponseEntity.ok(toMap(c));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClassroom(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String id) {
        try {
            classroomService.deleteClassroom(principal.teacherId(), id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============== 诗词管理 ==============
    @PostMapping("/{id}/poems")
    public ResponseEntity<?> addPoem(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String id,
            @RequestBody Map<String, String> poem) {
        try {
            // 校验必填字段
            String title = poem.get("title");
            String author = poem.get("author");
            String dynasty = poem.get("dynasty");
            String content = poem.get("content");
            if (title == null || author == null || content == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "title、author、content必填"));
            }
            if (dynasty == null) dynasty = "未知";
            poem.put("dynasty", dynasty);
            
            var c = classroomService.addPoem(principal.teacherId(), id, poem);
            return ResponseEntity.ok(toMap(c));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/poems/{title}")
    public ResponseEntity<?> removePoem(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String id,
            @PathVariable String title) {
        try {
            var c = classroomService.removePoem(principal.teacherId(), id, title);
            return ResponseEntity.ok(toMap(c));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(com.classpet.entity.Classroom c) {
        return Map.of(
            "id", c.getId(),
            "name", c.getName(),
            "type", c.getType(),
            "config", c.getConfig(),
            "createdAt", c.getCreatedAt().toString()
        );
    }
}
