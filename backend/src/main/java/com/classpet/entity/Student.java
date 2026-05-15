package com.classpet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 20)
    private String name;

    // 学号，唯一标识，用于学生登录
    @Column(name = "student_no", unique = true, length = 20)
    private String studentNo;

    @Column(nullable = false)
    private String teacherId;

    private Integer petId;
    private String petName;
    private String petIcon;
    private Integer food = 0;

    // 可用精灵球数量（每月发放，可叠加）
    private Integer pokemonBalls = 0;

    // 已装备的商品ID列表，JSON数组格式，如 ["uuid1","uuid2"]
    @Column(name = "equipped_items", length = 500)
    private String equippedItems = "[]";

    // 宠物更换卡数量
    @Column(name = "pet_change_cards")
    private Integer petChangeCards = 0;

    // 学生展示的代表宝可梦ID
    @Column(name = "represent_pokemon_id")
    private String representPokemonId;

    // 进化道具库存，JSON对象格式，如 {"水之石":1,"火之石":2}
    @Column(name = "evolution_items", length = 1000)
    private String evolutionItems = "{}";

    private LocalDateTime createdAt = LocalDateTime.now();

    public Student() {}
    public Student(String name, String teacherId) {
        this.name = name;
        this.teacherId = teacherId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public Integer getPetId() { return petId; }
    public void setPetId(Integer petId) { this.petId = petId; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getPetIcon() { return petIcon; }
    public void setPetIcon(String petIcon) { this.petIcon = petIcon; }
    public Integer getFood() { return food; }
    public void setFood(Integer food) { this.food = food; }

    public Integer getPokemonBalls() { return pokemonBalls; }
    public void setPokemonBalls(Integer pokemonBalls) { this.pokemonBalls = pokemonBalls; }
    public String getEquippedItems() { return equippedItems; }
    public void setEquippedItems(String equippedItems) { this.equippedItems = equippedItems; }
    public Integer getPetChangeCards() { return petChangeCards; }
    public void setPetChangeCards(Integer petChangeCards) { this.petChangeCards = petChangeCards; }
    public String getRepresentPokemonId() { return representPokemonId; }
    public void setRepresentPokemonId(String representPokemonId) { this.representPokemonId = representPokemonId; }
    public String getEvolutionItems() { return evolutionItems; }
    public void setEvolutionItems(String evolutionItems) { this.evolutionItems = evolutionItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
