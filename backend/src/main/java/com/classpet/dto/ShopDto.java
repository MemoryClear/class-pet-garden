package com.classpet.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.classpet.entity.ShopItem;
import com.classpet.entity.ExchangeRecord;

public class ShopDto {

    @Data
    public static class ShopItemRequest {
        @NotBlank(message = "商品名不能为空")
        private String name;

        @NotBlank(message = "图标不能为空")
        private String icon;

        @NotNull(message = "价格不能为空")
        @Min(value = 1, message = "价格至少为1")
        private Integer price;

        private String description;

        @NotNull(message = "库存不能为空")
        @Min(value = 0, message = "库存不能为负数")
        private Integer stock;
    }

    @Data
    public static class ExchangeRequest {
        @NotBlank(message = "学生ID不能为空")
        private String studentId;

        @NotBlank(message = "商品ID不能为空")
        private String itemId;
    }

    @Data
    public static class GiftRequest {
        @NotBlank(message = "赠送者学生ID不能为空")
        private String fromStudentId;

        @NotBlank(message = "接收者学生ID不能为空")
        private String toStudentId;

        @NotBlank(message = "兑换记录ID不能为空")
        private String recordId;
    }

    @Data
    public static class ShopItemResponse {
        private String id;
        private String name;
        private String icon;
        private Integer price;
        private String description;
        private Integer stock;
        private LocalDateTime createdAt;

        public static ShopItemResponse from(ShopItem item) {
            ShopItemResponse res = new ShopItemResponse();
            res.setId(item.getId());
            res.setName(item.getName());
            res.setIcon(item.getIcon());
            res.setPrice(item.getPrice());
            res.setDescription(item.getDescription());
            res.setStock(item.getStock());
            res.setCreatedAt(item.getCreatedAt());
            return res;
        }
    }

    @Data
    public static class ExchangeRecordResponse {
        private String id;
        private String studentId;
        private String studentName;
        private String itemId;
        private String itemName;
        private String itemIcon;
        private Integer foodSpent;
        private String giftFrom;      // 赠送者ID
        private String giftFromName;  // 赠送者名字
        private LocalDateTime createdAt;

        public static ExchangeRecordResponse from(ExchangeRecord record) {
            ExchangeRecordResponse res = new ExchangeRecordResponse();
            res.setId(record.getId());
            res.setStudentId(record.getStudentId());
            res.setStudentName(record.getStudentName());
            res.setItemId(record.getItemId());
            res.setItemName(record.getItemName());
            res.setItemIcon(record.getItemIcon());
            res.setFoodSpent(record.getFoodSpent());
            res.setGiftFrom(record.getGiftFrom());
            res.setGiftFromName(record.getGiftFromName());
            res.setCreatedAt(record.getCreatedAt());
            return res;
        }
    }
}