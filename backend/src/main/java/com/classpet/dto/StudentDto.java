package com.classpet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class StudentDto {
    public static class CreateRequest {
        @NotBlank @Size(max = 10)
        public String name;
        // 初始密码，可选；留空 = 默认 = 学号 + 强制改密
        @Size(min = 0, max = 50)
        public String initialPassword;
    }
    public static class UpdateRequest {
        @Size(max = 10)
        public String name;
        @Size(max = 20)
        public String studentNo;
    }
    public static class BatchCreateRequest {
        @NotBlank
        public String names; // newline-separated names
        // 统一初始密码，可选；留空 = 默认 = 学号 + 强制改密
        @Size(min = 0, max = 50)
        public String initialPassword;
    }
    public static class ResetPasswordRequest {
        // 留空 = 重置为学号 + 强制改密
        @Size(min = 0, max = 50)
        public String newPassword;
    }
    public static class BatchResetPasswordRequest {
        @NotNull
        public List<String> studentIds;
        // 留空 = 重置为学号 + 强制改密
        @Size(min = 0, max = 50)
        public String newPassword;
    }
    public static class AdoptRequest {
        public Integer petId;
        public String petName;
        public String petIcon;
    }
    public static class ScoreRequest {
        public String scoreItemId;
        // 倍数（可选，默认 1）。>1 时记为一条聚合记录，名称后缀"×N"，point = item.point × N
        public Integer multiplier;
    }
    // 装备商品请求
    public static class EquipRequest {
        public String itemId; // ShopItem UUID
    }
}