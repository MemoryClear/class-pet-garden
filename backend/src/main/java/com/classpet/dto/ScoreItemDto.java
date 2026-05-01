package com.classpet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public class ScoreItemDto {
    public static class CreateRequest {
        @NotBlank @Size(max = 10)
        public String icon;
        @NotBlank @Size(max = 50)
        public String name;
        @NotNull
        public Integer point;
    }
}