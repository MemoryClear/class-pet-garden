package com.classpet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_pokemon")
public class StudentPokemon {
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(length = 36, nullable = false)
    private String studentId;
    
    @Column(nullable = false)
    private Integer pokedexId;
    
    @Column(length = 50, nullable = false)
    private String name;
    
    @Column(length = 255)
    private String image;
    
    @Column(length = 255)
    private String types;  // JSON array
    
    @Column(nullable = false)
    private Integer food = 0;
    
    @Column(nullable = false)
    private Integer evolutionStage = 0;  // 0=未进化, 1=1阶, 2=2阶
    
    @Column(length = 50)
    private String formName;  // For Mega/Gigantamax forms (e.g. 超级妙蛙花, 超极巨化妙蛙花)
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructor for creating new Pokemon
    public StudentPokemon(String studentId, Integer pokedexId, String name, String image, String types, String formName) {
        this.id = UUID.randomUUID().toString();
        this.studentId = studentId;
        this.pokedexId = pokedexId;
        this.name = name;
        this.image = image;
        this.types = types;
        this.formName = formName;
        this.food = 0;
        this.evolutionStage = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Default constructor
    public StudentPokemon() {}
    
    // 计算等级（每10食物升1级）
    public Integer getLevel() {
        return food == null ? 1 : Math.max(1, food / 10 + 1);
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Integer getPokedexId() { return pokedexId; }
    public void setPokedexId(Integer pokedexId) { this.pokedexId = pokedexId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getTypes() { return types; }
    public void setTypes(String types) { this.types = types; }
    public Integer getFood() { return food; }
    public void setFood(Integer food) { this.food = food; }
    public Integer getEvolutionStage() { return evolutionStage; }
    public void setEvolutionStage(Integer evolutionStage) { this.evolutionStage = evolutionStage; }
    public String getFormName() { return formName; }
    public void setFormName(String formName) { this.formName = formName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}