package com.classpet.controller;

import com.classpet.entity.ReadingRecord;
import com.classpet.entity.ReadingSetting;
import com.classpet.repository.ReadingRecordRepository;
import com.classpet.repository.ReadingSettingRepository;
import com.classpet.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reading")
public class ReadingController {
    
    @Autowired
    private ReadingRecordRepository recordRepository;
    
    @Autowired
    private ReadingSettingRepository settingRepository;
    
    @Autowired
    private StudentService studentService;
    
    @PostMapping("/record")
    public ResponseEntity<?> recordReading(@RequestBody Map<String, String> request) {
        String studentId = request.get("studentId");
        String activityType = request.get("activityType");
        String itemId = request.get("itemId");
        String teacherId = request.get("teacherId");
        
        System.out.println("[ReadingRecord] studentId=" + studentId + ", activityType=" + activityType + ", itemId=" + itemId + ", teacherId=" + teacherId);
        if (studentId == null || activityType == null || itemId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
        }
        
        LocalDate today = LocalDate.now();
        
        int dailyMax = 10;
        if (teacherId != null) {
            Optional<ReadingSetting> setting = settingRepository
                .findByTeacherIdAndActivityType(teacherId, activityType);
            if (setting.isPresent()) {
                if (!setting.get().getEnabled()) {
                    return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "该活动已禁用"
                    ));
                }
                dailyMax = setting.get().getDailyMaxScore();
            }
        }
        
        Integer todayScore = recordRepository.sumScoreByStudentAndActivityAndItemAndDate(
            studentId, activityType, itemId, today);
        if (todayScore == null) todayScore = 0;
        
        if (todayScore >= dailyMax) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "今日已达积分上限",
                "currentScore", todayScore,
                "maxScore", dailyMax
            ));
        }
        
        // "ALL" itemId: check total across all items for this activity type today
        int scoreToAdd = 1;
        if ("ALL".equals(itemId)) {
            Integer totalAllToday = recordRepository.sumScoreByStudentAndActivityAndDate(
                studentId, activityType, today);
            int alreadyGot = totalAllToday != null ? totalAllToday : 0;
            int remaining = dailyMax - alreadyGot;
            if (remaining <= 0) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "今日已达积分上限",
                    "currentScore", alreadyGot,
                    "maxScore", dailyMax
                ));
            }
            scoreToAdd = remaining; // give the rest up to max
        }
        
        // 保存打卡记录
        ReadingRecord record = new ReadingRecord();
        record.setStudentId(studentId);
        record.setActivityType(activityType);
        record.setItemId(itemId);
        record.setRecordDate(today);
        record.setScore(scoreToAdd);
        recordRepository.save(record);
        
        // 给学生加积分！
        String reason = getActivityLabel(activityType) + " " + itemId;
        try {
            studentService.addScoreForQuiz(studentId, scoreToAdd, reason);
        } catch (IllegalArgumentException e) {
            System.err.println("[ReadingRecord] addScore failed: " + e.getMessage() + ", studentId=" + studentId);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "学生不存在: " + studentId
            ));
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "打卡成功，+" + scoreToAdd + "分",
            "scoreAdded", scoreToAdd,
            "newScore", todayScore + scoreToAdd,
            "maxScore", dailyMax
        ));
    }
    
    private String getActivityLabel(String activityType) {
        return switch (activityType) {
            case "POEM_READING" -> "📖 诗词朗读";
            case "PINYIN_CARD" -> "🔤 拼音朗读";
            case "PINYIN_TONE" -> "🎵 声调朗读";
            case "MULTIPLY_CELL" -> "✖️ 乘法口诀";
            case "ENGLISH_LETTER" -> "🔤 英语字母";
            default -> "📚 朗读打卡";
        };
    }
    
    @GetMapping("/stats/{studentId}")
    public ResponseEntity<?> getStats(@PathVariable String studentId,
                                      @RequestParam(required = false) String activityType) {
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new HashMap<>();
        
        if (activityType != null) {
            Integer score = recordRepository.sumScoreByStudentAndActivityAndDate(
                studentId, activityType, today);
            result.put("activityType", activityType);
            result.put("todayScore", score != null ? score : 0);
        } else {
            String[] activities = {"POEM_READING", "PINYIN_CARD", "PINYIN_TONE", "MULTIPLY_CELL", "ENGLISH_LETTER"};
            for (String act : activities) {
                Integer score = recordRepository.sumScoreByStudentAndActivityAndDate(
                    studentId, act, today);
                result.put(act, score != null ? score : 0);
            }
        }
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/settings/{teacherId}")
    public ResponseEntity<?> getSettings(@PathVariable String teacherId) {
        List<ReadingSetting> settings = settingRepository.findByTeacherId(teacherId);
        return ResponseEntity.ok(settings);
    }
    
    @PostMapping("/settings")
    public ResponseEntity<?> saveSetting(@RequestBody ReadingSetting setting) {
        if (setting.getTeacherId() == null || setting.getActivityType() == null) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }
        
        Optional<ReadingSetting> existing = settingRepository
            .findByTeacherIdAndActivityType(setting.getTeacherId(), setting.getActivityType());
        
        if (existing.isPresent()) {
            ReadingSetting s = existing.get();
            s.setDailyMaxScore(setting.getDailyMaxScore());
            s.setEnabled(setting.getEnabled());
            settingRepository.save(s);
            return ResponseEntity.ok(s);
        } else {
            settingRepository.save(setting);
            return ResponseEntity.ok(setting);
        }
    }
}
