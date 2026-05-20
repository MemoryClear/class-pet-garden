package com.classpet.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_records")
public class ReadingRecord {
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(length = 36, nullable = false)
    private String studentId;
    
    @Column(length = 50, nullable = false)
    private String activityType; // POEM_READING, PINYIN_CARD, MULTIPLY_CELL, ENGLISH_LETTER
    
    @Column(length = 100, nullable = false)
    private String itemId; // poem_id, "shengmu_b", "1x1", "A"
    
    @Column(nullable = false)
    private LocalDate recordDate;
    
    @Column(nullable = false)
    private Integer score = 1;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
