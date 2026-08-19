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

    // 赠送去向（null表示未转出，非null表示当前学生转给了谁——仅送出方记录有值）
    @Column(name = "gift_to")
    private String giftTo;

    @Column(name = "gift_to_name")
    private String giftToName;

    @Column(name = "action_type")
    private String actionType = ActionType.PURCHASE.name();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ActionType {
        PURCHASE,    // 兑换（学生自己买/教师帮买，foodSpent > 0）
        GIFT_OUT,    // 赠出（studentId=赠出方，giftTo=接收方，foodSpent=0）
        GIFT_IN,     // 收到（studentId=接收方，giftFrom=赠出方，foodSpent=0）
        REVOKED      // 撤销（保留原字段+标识）
    }
}