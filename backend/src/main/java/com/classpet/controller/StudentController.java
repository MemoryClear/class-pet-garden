package com.classpet.controller;

import com.classpet.dto.StudentDto;
import com.classpet.entity.Student;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedTeacher;
import com.classpet.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired private StudentService studentService;

    @GetMapping
    public ResponseEntity<List<Student>> getStudents(
            @AuthenticationPrincipal AuthenticatedTeacher principal) {
        return ResponseEntity.ok(studentService.getStudents(principal.teacherId()));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<Student>> getLeaderboard(
            @AuthenticationPrincipal AuthenticatedTeacher principal) {
        return ResponseEntity.ok(studentService.getLeaderboard(principal.teacherId()));
    }

    @GetMapping("/leaderboard/total")
    public ResponseEntity<List<Map<String, Object>>> getTotalScoreLeaderboard(
            @AuthenticationPrincipal AuthenticatedTeacher principal) {
        return ResponseEntity.ok(studentService.getTotalScoreLeaderboard(principal.teacherId()));
    }

    @PostMapping
    public ResponseEntity<?> createStudent(
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @Valid @RequestBody StudentDto.CreateRequest req) {
        try {
            return ResponseEntity.ok(studentService.createStudent(req.name, principal.teacherId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<?> batchCreate(
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @Valid @RequestBody StudentDto.BatchCreateRequest req) {
        try {
            List<String> names = Arrays.stream(req.names.split("\n"))
                    .map(s -> s.trim())
                    .filter(s -> !s.isEmpty() && s.length() <= 10)
                    .collect(Collectors.toList());
            if (names.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "未提供有效姓名"));
            }
            return ResponseEntity.ok(studentService.batchCreateStudents(names, principal.teacherId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @Valid @RequestBody StudentDto.UpdateRequest req) {
        try {
            return ResponseEntity.ok(studentService.updateStudent(id, principal.teacherId(), req.name));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedTeacher principal) {
        try {
            studentService.deleteStudent(id, principal.teacherId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/adopt")
    public ResponseEntity<?> adoptPet(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @RequestBody StudentDto.AdoptRequest req) {
        try {
            return ResponseEntity.ok(studentService.adoptPet(
                    id, principal.teacherId(), req.petId, req.petName, req.petIcon));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/assign-pets")
    public ResponseEntity<?> assignPetsRandomly(
            @AuthenticationPrincipal AuthenticatedTeacher principal) {
        return ResponseEntity.ok(studentService.assignPetsRandomly(principal.teacherId()));
    }

    // 装备商品
    @PostMapping("/{id}/equip")
    public ResponseEntity<?> equipItem(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @RequestBody StudentDto.EquipRequest req) {
        try {
            return ResponseEntity.ok(studentService.equipItem(id, principal.teacherId(), req.itemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 卸下商品
    @DeleteMapping("/{id}/equip/{itemId}")
    public ResponseEntity<?> unequipItem(
            @PathVariable String id,
            @PathVariable String itemId,
            @AuthenticationPrincipal AuthenticatedTeacher principal) {
        try {
            return ResponseEntity.ok(studentService.unequipItem(id, principal.teacherId(), itemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/score")
    public ResponseEntity<?> applyScore(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @RequestBody StudentDto.ScoreRequest req) {
        try {
            return ResponseEntity.ok(studentService.applyScore(id, principal.teacherId(), req.scoreItemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}