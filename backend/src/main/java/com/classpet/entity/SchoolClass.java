package com.classpet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 班级。
 *
 * <p>一个教师可以拥有多个班级；学生、积分历史、兑换记录都挂在班级下。
 * 学号格式为「班级号-S0001」，其中 code 即本实体的 {@link #code}。</p>
 *
 * <p>注意：与 {@link Classroom}（语文/数学/英语"课堂"）是两个概念，别混。</p>
 */
@Entity
@Table(name = "school_classes", indexes = {
        @Index(name = "idx_class_teacher", columnList = "teacher_id")
})
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "teacher_id", nullable = false)
    private String teacherId;

    /** 班级名称，如「初一(3)班」 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 班级号，学号前缀，如「C7F3」，全局唯一 */
    @Column(nullable = false, unique = true, length = 12)
    private String code;

    /** 是否出现在公开看板 */
    @Column(name = "show_on_board", nullable = false, columnDefinition = "boolean default false")
    private Boolean showOnBoard = false;

    /** 排序，数字小的在前 */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SchoolClass() {}

    public SchoolClass(String teacherId, String name, String code) {
        this.teacherId = teacherId;
        this.name = name;
        this.code = code;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Boolean getShowOnBoard() { return showOnBoard; }
    public void setShowOnBoard(Boolean showOnBoard) { this.showOnBoard = showOnBoard; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
