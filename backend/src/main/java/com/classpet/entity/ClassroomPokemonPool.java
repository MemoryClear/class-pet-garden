package com.classpet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 班级宝可梦池
 * 
 * 教师每月可设置当前班级学生可领取的宝可梦范围及概率权重
 * 学生使用精灵球时从池中按权重随机获得一只
 */
@Entity
@Table(name = "classroom_pokemon_pool")
public class ClassroomPokemonPool {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    // 班级ID
    @Column(length = 36, nullable = false)
    private String classroomId;
    
    // 宝可梦池条目列表（JSON数组，每项含pokedexId和weight）
    // 例如: [{"pokedexId":1,"weight":1.0},{"pokedexId":4,"weight":2.0}]
    // weight为相对权重，权重越高被抽中的概率越大
    @Column(name = "pokedex_entries", length = 2000)
    private String pokedexEntries = "[]";
    
    // 更新时间
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public ClassroomPokemonPool() {
        this.pokedexEntries = "[]";
    }
    
    public ClassroomPokemonPool(String classroomId) {
        this.classroomId = classroomId;
        this.pokedexEntries = "[]";
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getPokedexEntries() { return pokedexEntries; }
    public void setPokedexEntries(String entries) { this.pokedexEntries = entries; }
    
    // 为兼容旧数据，提供便利方法
    public String getPokedexIds() {
        if (pokedexEntries == null || pokedexEntries.isEmpty()) return "[]";
        try {
            List<Map<String, Object>> entries = new ObjectMapper().readValue(pokedexEntries,
                new TypeReference<List<Map<String, Object>>>() {});
            List<Integer> ids = new ArrayList<>();
            for (Map<String, Object> e : entries) {
                if (e.get("pokedexId") != null) ids.add(((Number) e.get("pokedexId")).intValue());
            }
            return new ObjectMapper().writeValueAsString(ids);
        } catch (Exception ex) { return "[]"; }
    }
    
    public void setPokedexIds(String ids) {
        if (ids == null || ids.isEmpty()) { this.pokedexEntries = "[]"; return; }
        try {
            List<Integer> idList = new ObjectMapper().readValue(ids, List.class);
            List<Map<String, Object>> entries = new ArrayList<>();
            for (Integer id : idList) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("pokedexId", id);
                entry.put("weight", 1.0);
                entries.add(entry);
            }
            this.pokedexEntries = new ObjectMapper().writeValueAsString(entries);
        } catch (Exception ex) { this.pokedexEntries = "[]"; }
    }
}
