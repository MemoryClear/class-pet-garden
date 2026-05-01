package com.classpet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "score_items")
public class ScoreItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 10)
    private String icon;

    @Column(nullable = false, length = 50)
    private String name;

    private Integer point;

    @Column(nullable = false)
    private String teacherId;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ScoreItem() {}
    public ScoreItem(String icon, String name, Integer point, String teacherId) {
        this.icon = icon;
        this.name = name;
        this.point = point;
        this.teacherId = teacherId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getPoint() { return point; }
    public void setPoint(Integer point) { this.point = point; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}