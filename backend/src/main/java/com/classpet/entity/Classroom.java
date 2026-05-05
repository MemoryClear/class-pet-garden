package com.classpet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "classrooms")
public class Classroom {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String teacherId;

    // 教室名称：语文小课堂 / 数学小课堂 / 英语小课堂
    @Column(nullable = false, length = 50)
    private String name;

    // 类型：CHINESE / MATH / ENGLISH
    @Column(nullable = false, length = 20)
    private String type;

    // 配置 JSON，根据 type 不同内容不同
    // CHINESE: { "poems": [...] }
    // MATH: { "operations": ["+","-","×","÷"], "maxNum": 100 }
    // ENGLISH: {}
    @Column(columnDefinition = "TEXT")
    private String config = "{}";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // getter / setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}