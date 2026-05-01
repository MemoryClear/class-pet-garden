package com.classpet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StudentDto {
    public static class CreateRequest {
        @NotBlank @Size(max = 10)
        public String name;
    }
    public static class UpdateRequest {
        @Size(max = 10)
        public String name;
    }
    public static class BatchCreateRequest {
        @NotBlank
        public String names; // newline-separated names
    }
    public static class AdoptRequest {
        public Integer petId;
        public String petName;
        public String petIcon;
    }
    public static class ScoreRequest {
        public String scoreItemId;
    }
    // 装备商品请求
    public static class EquipRequest {
        public String itemId; // ShopItem UUID
    }
}
