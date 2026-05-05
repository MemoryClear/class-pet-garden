package com.classpet.controller;

import com.classpet.dto.AuthDto;
import com.classpet.security.JwtTokenProvider;
import com.classpet.entity.Student;
import com.classpet.entity.Teacher;
import com.classpet.repository.StudentRepository;
import com.classpet.repository.TeacherRepository;
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
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

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

    // 学生登录：学号 + 教师密码
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
            // 查找学生
            Student student = studentRepository.findByStudentNo(studentNo)
                    .orElseThrow(() -> new IllegalArgumentException("学号不存在"));
            // 用教师的密码验证
            Teacher teacher = teacherRepository.findById(student.getTeacherId())
                    .orElseThrow(() -> new IllegalArgumentException("教师账号不存在"));
            if (!passwordEncoder.matches(password, teacher.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("error", "密码错误"));
            }
            // 生成学生token（包含学生ID，角色为student）
            String token = tokenProvider.generateStudentToken(
                    student.getName(), student.getTeacherId(), student.getId());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", "student",
                    "studentId", student.getId(),
                    "studentName", student.getName(),
                    "studentNo", student.getStudentNo(),
                    "teacherId", student.getTeacherId()
            ));
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