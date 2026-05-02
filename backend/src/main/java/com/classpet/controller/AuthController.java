package com.classpet.controller;

import com.classpet.dto.AuthDto;
import com.classpet.security.JwtTokenProvider;
import com.classpet.service.AuthService;
import com.classpet.service.ScoreItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private ScoreItemService scoreItemService;
    @Autowired private com.classpet.service.ShopService shopService;

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        System.out.println("Body: " + body);
        try {
            String username = body.get("username");
            String password = body.get("password");
            String confirmPassword = body.get("confirmPassword");
            
            AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
            req.username = username;
            req.password = password;
            req.confirmPassword = confirmPassword;
            
            AuthDto.LoginResponse resp = authService.register(req);
            scoreItemService.initializeDefaults(resp.teacherId);
            shopService.initializeDefaults(resp.teacherId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            
            AuthDto.LoginRequest req = new AuthDto.LoginRequest();
            req.username = username;
            req.password = password;
            
            return ResponseEntity.ok(authService.login(req));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "Missing token"));
            }
            String token = authHeader.substring(7);
            if (!tokenProvider.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
            }
            String username = tokenProvider.getUsernameFromToken(token);
            String teacherId = tokenProvider.getTeacherIdFromToken(token);
            AuthDto.LoginResponse resp = authService.activate(username, teacherId, body.get("code"));
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("valid", false, "error", "Missing token"));
        }
        String token = authHeader.substring(7);
        if (tokenProvider.validateToken(token)) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "username", tokenProvider.getUsernameFromToken(token),
                    "teacherId", tokenProvider.getTeacherIdFromToken(token)));
        }
        return ResponseEntity.status(401).body(Map.of("valid", false, "error", "Invalid token"));
    }
}