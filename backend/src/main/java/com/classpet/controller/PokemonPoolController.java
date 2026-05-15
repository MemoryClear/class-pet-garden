package com.classpet.controller;

import com.classpet.config.MonthlyBallScheduler;
import com.classpet.entity.ClassroomPokemonPool;
import com.classpet.repository.ClassroomPokemonPoolRepository;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 宝可梦池管理控制器（教师端）
 */
@RestController
@RequestMapping("/api/pokemon")
public class PokemonPoolController {

    @Autowired
    private ClassroomPokemonPoolRepository poolRepository;
    
    @Autowired
    private MonthlyBallScheduler monthlyBallScheduler;

    /**
     * 获取班级的宝可梦池
     */
    @GetMapping("/classroom-pool")
    public ResponseEntity<?> getClassroomPool(
            @RequestParam String classroomId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        
        // 验证教师权限（班级ID = 教师ID）
        if (principal.isStudent() || principal.teacherId() == null || !principal.teacherId().equals(classroomId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限访问该班级"));
        }
        
        ClassroomPokemonPool pool = poolRepository.findByClassroomId(classroomId)
                .orElse(null);
        
        if (pool == null) {
            return ResponseEntity.ok(Map.of("pokedexEntries", List.of()));
        }
        
        return ResponseEntity.ok(Map.of(
                "classroomId", pool.getClassroomId(),
                "pokedexEntries", pool.getPokedexEntries(),
                "updatedAt", pool.getUpdatedAt()
        ));
    }

    /**
     * 设置班级的宝可梦池（教师）- 支持权重
     * body: { classroomId, pokedexEntries: [{"pokedexId":1,"weight":2.0},...] }
     */
    @PostMapping("/classroom-pool")
    public ResponseEntity<?> setClassroomPool(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        
        String classroomId = body.get("classroomId") != null ? body.get("classroomId").toString() : null;
        Object entriesObj = body.get("pokedexEntries");
        String entriesJson;
        if (entriesObj instanceof String) {
            entriesJson = (String) entriesObj;
        } else if (entriesObj instanceof Iterable) {
            StringBuilder sb = new StringBuilder("[");
            int i = 0;
            for (Object item : (Iterable) entriesObj) {
                if (i > 0) sb.append(",");
                if (item instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) item;
                    sb.append("{\"pokedexId\"");
                    sb.append(":");
                    sb.append(m.get("pokedexId"));
                    sb.append(",\"weight\"");
                    sb.append(":");
                    sb.append(m.get("weight"));
                    sb.append("}");
                }
                i++;
            }
            sb.append("]");
            entriesJson = sb.toString();
        } else {
            entriesJson = "[]";
        }
        
        if (classroomId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "classroomId不能为空"));
        }
        
        // 验证教师权限
        if (principal.isStudent() || principal.teacherId() == null || !principal.teacherId().equals(classroomId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限设置该班级"));
        }
        
        // 查找现有池或创建新池
        ClassroomPokemonPool pool = poolRepository.findByClassroomId(classroomId)
                .orElse(new ClassroomPokemonPool(classroomId));
        
        pool.setPokedexEntries(entriesJson);
        poolRepository.save(pool);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "宝可梦池设置成功",
                "pokedexEntries", pool.getPokedexEntries()
        ));
    }
    
    /**
     * 清空班级的宝可梦池（恢复到默认Gen1）
     */
    @DeleteMapping("/classroom-pool")
    public ResponseEntity<?> clearClassroomPool(
            @RequestParam String classroomId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        
        if (principal.isStudent() || principal.teacherId() == null || !principal.teacherId().equals(classroomId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限操作该班级"));
        }
        
        poolRepository.findByClassroomId(classroomId).ifPresent(pool -> {
            pool.setPokedexEntries("[]");
            poolRepository.save(pool);
        });
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "宝可梦池已清空，将使用默认Gen1池"
        ));
    }
    
    /**
     * 手动发放精灵球（教师可随时触发）
     * 给当前教师班级所有学生各发放1个精灵球
     */
    @PostMapping("/distribute-balls")
    public ResponseEntity<?> distributeBalls(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限教师使用"));
        }
        int count = monthlyBallScheduler.manualDistribute();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "精灵球发放成功",
                "studentsAffected", count
        ));
    }
}
