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

    @Column(nullable = false)
    private String teacherId;

    private Integer petId;
    private String petName;
    private String petIcon;
    private Integer food = 0;

    // 已装备的商品ID列表，JSON数组格式，如 ["uuid1","uuid2"]
    @Column(name = "equipped_items", length = 500)
    private String equippedItems = "[]";

    // 宠物更换卡数量
    @Column(name = "pet_change_cards")
    private Integer petChangeCards = 0;

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
    public String getEquippedItems() { return equippedItems; }
    public void setEquippedItems(String equippedItems) { this.equippedItems = equippedItems; }
    public Integer getPetChangeCards() { return petChangeCards; }
    public void setPetChangeCards(Integer petChangeCards) { this.petChangeCards = petChangeCards; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
