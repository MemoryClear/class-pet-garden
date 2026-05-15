package com.classpet.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "shop_items")
public class ShopItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String icon;

    @Column(nullable = false)
    private Integer price;

    private String description;

    @Column(nullable = false)
    private Integer stock = 0;

    // 商品类型: decoration(装饰道具) | petCard(宠物更换卡) | evolution_item(进化道具)
    @Column(name = "item_type")
    private String itemType = "decoration";

    // 进化道具名称（如"水之石"），仅 itemType=evolution_item 时有效
    @Column(name = "evolution_item_key", length = 50)
    private String evolutionItemKey;

    @Column(name = "teacher_id", nullable = false)
    private String teacherId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}