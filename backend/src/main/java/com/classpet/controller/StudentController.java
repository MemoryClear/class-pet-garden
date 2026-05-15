package com.classpet.controller;

import com.classpet.dto.StudentDto;
import com.classpet.entity.Student;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedUser;
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
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(studentService.getStudents(principal.teacherId()));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<Student>> getLeaderboard(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(studentService.getLeaderboard(principal.teacherId()));
    }

    @GetMapping("/leaderboard/total")
    public ResponseEntity<List<Map<String, Object>>> getTotalScoreLeaderboard(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(studentService.getTotalScoreLeaderboard(principal.teacherId()));
    }

    @PostMapping
    public ResponseEntity<?> createStudent(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody StudentDto.CreateRequest req) {
        try {
            return ResponseEntity.ok(studentService.createStudent(req.name, principal.teacherId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<?> batchCreate(
            @AuthenticationPrincipal AuthenticatedUser principal,
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
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody StudentDto.UpdateRequest req) {
        try {
            return ResponseEntity.ok(studentService.updateStudent(id, principal.teacherId(), req.name, req.studentNo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStudent(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
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
            @AuthenticationPrincipal AuthenticatedUser principal,
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
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(studentService.assignPetsRandomly(principal.teacherId()));
    }

    // 装备商品
    @PostMapping("/{id}/equip")
    public ResponseEntity<?> equipItem(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser principal,
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
            @AuthenticationPrincipal AuthenticatedUser principal) {
        try {
            return ResponseEntity.ok(studentService.unequipItem(id, principal.teacherId(), itemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/score")
    public ResponseEntity<?> applyScore(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody StudentDto.ScoreRequest req) {
        try {
            return ResponseEntity.ok(studentService.applyScore(id, principal.teacherId(), req.scoreItemId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 获取学生宝可梦数量
    @GetMapping("/{id}/pokemon-count")
    public ResponseEntity<?> getPokemonCount(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        try {
            long count = studentService.getPokemonCount(id);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 获取学生宝可梦列表（教师查看）
    @GetMapping("/{id}/pokemon")
    public ResponseEntity<?> getStudentPokemon(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        try {
            System.out.println("DEBUG CONTROLLER getStudentPokemon called with id: " + id);
            List<Map<String, Object>> pokemon = studentService.getStudentPokemon(id);
            // 获取学生当前代表宝可梦ID
            Student student = studentService.getStudentById(id);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("pokemonList", pokemon);
            result.put("representPokemonId", student.getRepresentPokemonId());
            result.put("petChangeCards", student.getPetChangeCards());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.out.println("DEBUG CONTROLLER ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 设置学生代表宝可梦（教师操作，消耗更换卡）
    @PutMapping("/{id}/represent-pokemon")
    public ResponseEntity<?> setRepresentPokemon(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody Map<String, Object> body) {
        try {
            String pokemonId = body.get("pokemonId") != null ? body.get("pokemonId").toString() : null;
            if (pokemonId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "pokemonId不能为空"));
            }
            Student student = studentService.setRepresentPokemon(id, pokemonId);
            return ResponseEntity.ok(Map.of("success", true, "student", student));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}