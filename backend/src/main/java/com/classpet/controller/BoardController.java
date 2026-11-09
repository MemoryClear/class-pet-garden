package com.classpet.controller;

import com.classpet.entity.SchoolClass;
import com.classpet.entity.Teacher;
import com.classpet.repository.ExchangeRecordRepository;
import com.classpet.repository.SchoolClassRepository;
import com.classpet.repository.ScoreHistoryRepository;
import com.classpet.repository.StudentRepository;
import com.classpet.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开看板（无需登录）：按「班级」维度展示排行榜与学生详情。
 *
 * <p>多班级改造后，看板的最小单位从"教师"变成"班级"：
 * 教师在某个班级开启 showOnBoard 后，该班级会单独出现在看板里。
 * 一个教师带三个班，就会看到三条看板条目。</p>
 */
@RestController
@RequestMapping("/api/board")
public class BoardController {

    @Autowired private TeacherRepository teacherRepo;
    @Autowired private StudentRepository studentRepo;
    @Autowired private ScoreHistoryRepository scoreHistoryRepo;
    @Autowired private ExchangeRecordRepository exchangeRecordRepo;
    @Autowired private SchoolClassRepository classRepo;

    /** 看板：所有开启公开展示的班级 */
    @GetMapping("/classes")
    public ResponseEntity<?> listBoardClasses() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SchoolClass c : classRepo.findByShowOnBoardTrue()) {
            String systemName = teacherRepo.findById(c.getTeacherId())
                    .map(Teacher::getSystemName)
                    .orElse("");
            list.add(Map.of(
                    "id", c.getId(),
                    "className", c.getName() != null ? c.getName() : "",
                    "systemName", systemName != null ? systemName : ""
            ));
        }
        return ResponseEntity.ok(list);
    }

    /** 排行榜：某班级前 10 名（按食物降序） */
    @GetMapping("/classes/{classId}/leaderboard")
    public ResponseEntity<?> leaderboard(@PathVariable String classId) {
        var clazz = classRepo.findById(classId)
                .filter(c -> Boolean.TRUE.equals(c.getShowOnBoard()));
        if (clazz.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "班级未加入看板或不存在"));
        }
        var students = studentRepo.findByClassIdOrderByFoodDesc(classId);
        List<Map<String, Object>> top = new ArrayList<>();
        int limit = Math.min(10, students.size());
        for (int i = 0; i < limit; i++) {
            var s = students.get(i);
            top.add(Map.of(
                    "id", s.getId(),
                    "name", s.getName() != null ? s.getName() : "",
                    "studentNo", s.getStudentNo() != null ? s.getStudentNo() : "",
                    "food", s.getFood() != null ? s.getFood() : 0,
                    "petName", s.getPetName() != null ? s.getPetName() : "",
                    "petIcon", s.getPetIcon() != null ? s.getPetIcon() : ""
            ));
        }
        return ResponseEntity.ok(top);
    }

    /** 学生详情：奖惩记录 + 礼物动向 + 宠物 */
    @GetMapping("/classes/{classId}/students/{studentId}/records")
    public ResponseEntity<?> studentRecords(
            @PathVariable String classId,
            @PathVariable String studentId) {
        var clazz = classRepo.findById(classId)
                .filter(c -> Boolean.TRUE.equals(c.getShowOnBoard()));
        if (clazz.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "班级未加入看板或不存在"));
        }
        var stu = studentRepo.findById(studentId).orElse(null);
        if (stu == null || !classId.equals(stu.getClassId())) {
            return ResponseEntity.status(404).body(Map.of("error", "学生不存在或不属于该班级"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", stu.getId());
        result.put("name", stu.getName());
        result.put("studentNo", stu.getStudentNo());
        result.put("food", stu.getFood() != null ? stu.getFood() : 0);
        if (stu.getPetName() != null || stu.getPetIcon() != null) {
            Map<String, Object> pet = new LinkedHashMap<>();
            pet.put("name", stu.getPetName());
            pet.put("icon", stu.getPetIcon());
            result.put("pet", pet);
        }

        // 奖惩（ScoreHistory）
        List<Map<String, Object>> scores = new ArrayList<>();
        for (var r : scoreHistoryRepo.findByStudentIdOrderByCreatedAtDesc(studentId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getScoreItemName());
            m.put("icon", r.getScoreItemIcon());
            m.put("point", r.getPoint());
            m.put("revoked", r.getRevoked() != null && r.getRevoked());
            m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            scores.add(m);
        }
        result.put("scores", scores);

        // 礼物（ExchangeRecord）
        List<Map<String, Object>> gifts = new ArrayList<>();
        for (var r : exchangeRecordRepo.findByStudentIdOrderByCreatedAtDesc(studentId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("itemName", r.getItemName());
            m.put("itemIcon", r.getItemIcon());
            m.put("foodSpent", r.getFoodSpent());
            m.put("giftFromName", r.getGiftFromName());
            m.put("giftToName", r.getGiftToName());
            m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            gifts.add(m);
        }
        result.put("gifts", gifts);

        return ResponseEntity.ok(result);
    }
}
