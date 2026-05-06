package com.classpet.controller;

import com.classpet.entity.Student;
import com.classpet.entity.ScoreHistory;
import com.classpet.entity.ExchangeRecord;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.classpet.service.StudentService;
import com.classpet.service.ShopService;
import com.classpet.repository.ShopItemRepository;
import com.classpet.repository.ScoreHistoryRepository;
import com.classpet.repository.ExchangeRecordRepository;
import com.classpet.entity.ShopItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 学生端专用API，学生登录后可访问
 * 只读操作为主，不能给自己加积分
 */
@RestController
@RequestMapping("/api/student")
public class StudentApiController {

    @Autowired private StudentService studentService;
    @Autowired private ShopItemRepository shopItemRepository;
    @Autowired private ScoreHistoryRepository scoreHistoryRepository;
    @Autowired private ExchangeRecordRepository exchangeRecordRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 获取自己的信息
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生访问"));
        }
        Student stu = studentService.getStudents(principal.teacherId()).stream()
                .filter(s -> s.getId().equals(principal.studentId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到学生信息"));
        
        // 计算宠物等级
        int food = stu.getFood() != null ? stu.getFood() : 0;
        String level;
        if (food >= 50) level = "传说期 🌟";
        else if (food >= 25) level = "成年期 💪";
        else if (food >= 10) level = "少年期 🎒";
        else level = "幼崽期 🥚";
        
        return ResponseEntity.ok(Map.of(
                "id", stu.getId(),
                "name", stu.getName(),
                "studentNo", stu.getStudentNo() != null ? stu.getStudentNo() : "",
                "petId", stu.getPetId() != null ? stu.getPetId() : 0,
                "petName", stu.getPetName() != null ? stu.getPetName() : "",
                "petIcon", stu.getPetIcon() != null ? stu.getPetIcon() : "",
                "food", food,
                "level", level,
                "petChangeCards", stu.getPetChangeCards() != null ? stu.getPetChangeCards() : 0,
                "equippedItems", stu.getEquippedItems() != null ? stu.getEquippedItems() : "[]"
        ));
    }

    // 光荣榜（只读）
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(studentService.getLeaderboard(principal.teacherId()));
    }

    // 总积分光荣榜（只读）
    @GetMapping("/leaderboard/total")
    public ResponseEntity<?> getTotalScoreLeaderboard(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(studentService.getTotalScoreLeaderboard(principal.teacherId()));
    }

    // 宠物图鉴
    @GetMapping("/pets")
    public ResponseEntity<?> getPetLibrary() {
        return ResponseEntity.ok(studentService.getPetLibrary());
    }

    // 等级说明
    @GetMapping("/level-guide")
    public ResponseEntity<?> getLevelGuide() {
        return ResponseEntity.ok(List.of(
                Map.of("level", "幼崽期 🥚", "range", "0-9分", "desc", "刚刚破壳，未来可期！"),
                Map.of("level", "少年期 🎒", "range", "10-24分", "desc", "茁壮成长，天天向上！"),
                Map.of("level", "成年期 💪", "range", "25-49分", "desc", "实力满满，独当一面！"),
                Map.of("level", "传说期 🌟", "range", "50分以上", "desc", "传说中的存在，无人能及！")
        ));
    }

    // 答题加分（仅限课堂答题使用）
    @PostMapping("/quiz-score")
    public ResponseEntity<?> addQuizScore(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody Map<String, Object> body) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生使用"));
        }
        int points = body.get("points") != null ? Integer.parseInt(body.get("points").toString()) : 1;
        String reason = body.get("reason") != null ? body.get("reason").toString() : "📚 课堂答题";
        Student stu = studentService.addScoreForQuiz(principal.studentId(), points, reason);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "food", stu.getFood(),
                "points", points
        ));
    }

    // 商品列表（只读，学生可浏览但不能兑换）
    @GetMapping("/shop")
    public ResponseEntity<?> getShopItems(@AuthenticationPrincipal AuthenticatedUser principal) {
        List<ShopItem> items = shopItemRepository.findByTeacherId(principal.teacherId());
        return ResponseEntity.ok(items);
    }

    // 兑换商品（学生可以用自己积分兑换）
    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeItem(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody Map<String, String> body) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生使用"));
        }
        String itemId = body.get("itemId");
        if (itemId == null || itemId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "商品ID不能为空"));
        }
        try {
            return ResponseEntity.ok(studentService.studentExchange(principal.studentId(), principal.teacherId(), itemId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 积分明细
    @GetMapping("/score-history")
    public ResponseEntity<?> getScoreHistory(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生使用"));
        }
        List<ScoreHistory> history = scoreHistoryRepository.findByStudentIdOrderByCreatedAtDesc(principal.studentId());
        return ResponseEntity.ok(history);
    }

    // 道具明细（含赠送记录）
    @GetMapping("/exchange-history")
    public ResponseEntity<?> getExchangeHistory(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生使用"));
        }
        List<ExchangeRecord> history = exchangeRecordRepository.findByStudentIdOrderByCreatedAtDesc(principal.studentId());
        return ResponseEntity.ok(history);
    }

    // 学生自助领养/更换宠物
    @PostMapping("/adopt")
    public ResponseEntity<?> adoptPet(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody Map<String, Object> body) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生使用"));
        }
        Integer petId = body.get("petId") != null ? Integer.parseInt(body.get("petId").toString()) : null;
        String petName = body.get("petName") != null ? body.get("petName").toString() : "";
        String petIcon = body.get("petIcon") != null ? body.get("petIcon").toString() : "";
        try {
            Student stu = studentService.studentAdoptPet(principal.studentId(), principal.teacherId(), petId, petName, petIcon);
            return ResponseEntity.ok(Map.of("success", true, "petId", stu.getPetId(), "petName", stu.getPetName(), "petIcon", stu.getPetIcon()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
