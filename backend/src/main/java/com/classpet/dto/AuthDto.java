package com.classpet.dto;

public class AuthDto {
    public static class LoginRequest {
        public String username;
        public String password;
    }
    public static class RegisterRequest {
        public String username;
        public String password;
        public String confirmPassword;
    }
    public static class ActivateRequest {
        public String code;
    }
    public static class LoginResponse {
        public String token;
        public String username;
        public String teacherId;
        public boolean activated;
        public String systemName;
        public String className;
        public String theme;
        public LoginResponse(String token, String username, String teacherId,
                             boolean activated, String systemName, String className, String theme) {
            this.token = token; this.username = username; this.teacherId = teacherId;
            this.activated = activated; this.systemName = systemName;
            this.className = className; this.theme = theme;
        }
    }
}