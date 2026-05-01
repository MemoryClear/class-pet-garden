package com.classpet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "score_history")
public class ScoreHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false, length = 50)
    private String studentName;

    @Column(nullable = false, length = 50)
    private String scoreItemName;

    @Column(length = 10)
    private String scoreItemIcon;

    private Integer point;

    @Column(nullable = false)
    private String teacherId;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean revoked = false;

    private LocalDateTime revokedAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ScoreHistory() {}
    public ScoreHistory(String studentId, String studentName, String scoreItemName,
                        String scoreItemIcon, Integer point, String teacherId) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.scoreItemName = scoreItemName;
        this.scoreItemIcon = scoreItemIcon;
        this.point = point;
        this.teacherId = teacherId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getScoreItemName() { return scoreItemName; }
    public void setScoreItemName(String scoreItemName) { this.scoreItemName = scoreItemName; }
    public String getScoreItemIcon() { return scoreItemIcon; }
    public void setScoreItemIcon(String scoreItemIcon) { this.scoreItemIcon = scoreItemIcon; }
    public Integer getPoint() { return point; }
    public void setPoint(Integer point) { this.point = point; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public Boolean getRevoked() { return revoked; }
    public void setRevoked(Boolean revoked) { this.revoked = revoked; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}