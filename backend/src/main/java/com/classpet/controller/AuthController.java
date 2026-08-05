package com.classpet.controller;

import com.classpet.dto.AuthDto;
import com.classpet.security.JwtTokenProvider;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.classpet.entity.Student;
import com.classpet.entity.Teacher;
import com.classpet.repository.StudentRepository;
import com.classpet.repository.TeacherRepository;
import com.classpet.service.AuthService;
import com.classpet.service.ScoreItemService;
import com.classpet.service.StudentService;
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
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired private StudentService studentService;

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
            
            AuthDto.LoginResponse resp = authService.login(req);
            // 补充旧账号缺失的进化道具
            shopService.migrateEvolutionItems(resp.teacherId);
            return ResponseEntity.ok(resp);
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

    // 学生登录：学号 + 学生自己的密码（取代旧版学号+教师密码模式）
    @PostMapping("/student-login")
    public ResponseEntity<?> studentLogin(@RequestBody Map<String, String> body) {
        try {
            String studentNo = body.get("studentNo");
            String password = body.get("password");
            if (studentNo == null || studentNo.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "请输入学号"));
            }
            if (password == null || password.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "请输入密码"));
            }
            Map<String, Object> info = studentService.studentLoginByPassword(studentNo, password);
            Student student = studentRepository.findById((String) info.get("studentId"))
                    .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
            String token = tokenProvider.generateStudentToken(
                    student.getName(), student.getTeacherId(), student.getId());
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("token", token);
            resp.put("role", "student");
            resp.put("studentId", info.get("studentId"));
            resp.put("studentName", info.get("studentName"));
            resp.put("studentNo", info.get("studentNo"));
            resp.put("teacherId", info.get("teacherId"));
            resp.put("mustChangePassword", info.get("mustChangePassword"));
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 学生自己修改密码（不需要旧密码，由 mustChangePassword 拦截保证安全）
    @PostMapping("/student-change-password")
    public ResponseEntity<?> studentChangePassword(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody Map<String, String> body) {
        try {
            if (principal == null || principal.studentId() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            String newPassword = body.get("newPassword");
            studentService.studentChangePassword(principal.studentId(), newPassword);
            return ResponseEntity.ok(Map.of("success", true));
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