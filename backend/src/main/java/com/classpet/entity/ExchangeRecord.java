package com.classpet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exchange_records")
public class ExchangeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "item_icon", nullable = false)
    private String itemIcon;

    @Column(name = "food_spent", nullable = false)
    private Integer foodSpent;

    @Column(name = "teacher_id", nullable = false)
    private String teacherId;

    // 赠送来源（null表示自己兑换，非null表示别人赠送）
    @Column(name = "gift_from")
    private String giftFrom;

    @Column(name = "gift_from_name")
    private String giftFromName;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}