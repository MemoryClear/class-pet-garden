package com.classpet.controller;

import com.classpet.config.MonthlyBallScheduler;
import com.classpet.entity.ClassroomPokemonPool;
import com.classpet.repository.ClassroomPokemonPoolRepository;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

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

    private final Map<Integer, Map<String, Object>> speciesMap = new HashMap<>();

    @PostConstruct
    public void loadSpecies() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("data/species.json").getInputStream();
            List<Map<String, Object>> list = mapper.readValue(is, List.class);
            for (Map<String, Object> s : list) {
                Integer id = ((Number) s.get("pokedexId")).intValue();
                speciesMap.put(id, s);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to load species.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    /**
     * 学生获取自己班级当月可抽取的宝可梦列表
     * 不返回权重信息（权重是教师端抽卡逻辑，对学生不透明）
     */
    @GetMapping("/classroom-pool/current")
    public ResponseEntity<?> getCurrentMonthPool(@AuthenticationPrincipal AuthenticatedUser principal) {
        String classroomId = principal.teacherId();
        if (classroomId == null || classroomId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "未找到班级信息"));
        }
        
        ClassroomPokemonPool pool = poolRepository.findByClassroomId(classroomId)
                .orElse(null);
        
        if (pool == null || pool.getPokedexEntries().isEmpty() || "[]".equals(pool.getPokedexEntries().trim())) {
            // 如果班级池为空或为空数组，返回默认的 Gen1 宝可梦列表
            return ResponseEntity.ok(Map.of(
                    "classroomId", classroomId,
                    "isDefault", true,
                    "pokemons", enrichPokemonList(getDefaultGen1Ids())
            ));
        }
        
        // 从 JSON 中提取 pokedexId 列表（不返回权重）
        List<Integer> pokedexIds = extractPokedexIds(pool.getPokedexEntries());
        if (pokedexIds.isEmpty()) {
            // 解析后也为空，返回默认 Gen1
            return ResponseEntity.ok(Map.of(
                    "classroomId", classroomId,
                    "isDefault", true,
                    "pokemons", enrichPokemonList(getDefaultGen1Ids())
            ));
        }
        
        return ResponseEntity.ok(Map.of(
                "classroomId", pool.getClassroomId(),
                "isDefault", false,
                "pokemons", enrichPokemonList(pokedexIds)
        ));
    }
    
    /**
     * 从 JSON 字符串中提取 pokedexId 列表
     */
    private List<Integer> extractPokedexIds(String json) {
        List<Integer> ids = new java.util.ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> entries = mapper.readValue(json, List.class);
            for (Map<String, Object> entry : entries) {
                Object pid = entry.get("pokedexId");
                if (pid instanceof Number) {
                    ids.add(((Number) pid).intValue());
                }
            }
        } catch (Exception e) {
            // 解析失败，返回空列表
        }
        return ids;
    }
    
    /**
     * 将 pokedexId 列表转为包含名称和图片的对象列表
     */
    private List<Map<String, Object>> enrichPokemonList(List<Integer> pokedexIds) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer id : pokedexIds) {
            Map<String, Object> s = speciesMap.get(id);
            if (s != null) {
                result.add(Map.of(
                    "pokedexId", id,
                    "name", s.getOrDefault("name", "未知"),
                    "image", s.getOrDefault("image", ""),
                    "types", s.getOrDefault("types", List.of())
                ));
            } else {
                result.add(Map.of(
                    "pokedexId", id,
                    "name", "未知",
                    "image", "",
                    "types", List.of()
                ));
            }
        }
        return result;
    }

    /**
     * 获取默认的 Gen1 宝可梦 ID 列表（1-151）
     */
    private List<Integer> getDefaultGen1Ids() {
        List<Integer> ids = new java.util.ArrayList<>();
        for (int i = 1; i <= 151; i++) {
            ids.add(i);
        }
        return ids;
    }
}
