package com.classpet.dto;

public class SettingsDto {
    public static class UpdateRequest {
        public String systemName;
        public String className;
        public String theme;
        /** light / dark */
        public String themeMode;
        /** 要修改名称的班级；为空则沿用旧的教师级 className */
        public String classId;
        public Boolean showOnBoard;
    }
}
