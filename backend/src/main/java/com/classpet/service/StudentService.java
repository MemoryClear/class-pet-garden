package com.classpet.service;

import com.classpet.entity.ScoreItem;
import com.classpet.entity.ScoreHistory;
import com.classpet.entity.Student;
import com.classpet.repository.ScoreHistoryRepository;
import com.classpet.repository.ScoreItemRepository;
import com.classpet.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.classpet.repository.ShopItemRepository;
import com.classpet.entity.ShopItem;

@Service
public class StudentService {

    @Autowired private StudentRepository studentRepository;
    @Autowired private ScoreItemRepository scoreItemRepository;
    @Autowired private ScoreHistoryRepository scoreHistoryRepository;
    @Autowired private ShopItemRepository shopItemRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Student> getStudents(String teacherId) {
        return studentRepository.findByTeacherIdOrderByCreatedAtAsc(teacherId);
    }

    @Transactional
    public Student createStudent(String name, String teacherId) {
        Student stu = new Student();
        stu.setName(name.trim());
        stu.setTeacherId(teacherId);
        return studentRepository.save(stu);
    }

    @Transactional
    public List<Student> batchCreateStudents(List<String> names, String teacherId) {
        return studentRepository.saveAll(names.stream().map(n -> {
            Student s = new Student();
            s.setName(n.trim());
            s.setTeacherId(teacherId);
            return s;
        }).toList());
    }

    @Transactional
    public Student updateStudent(String id, String teacherId, String name) {
        Student stu = findByIdAndTeacherId(id, teacherId);
        stu.setName(name.trim());
        return studentRepository.save(stu);
    }

    @Transactional
    public void deleteStudent(String id, String teacherId) {
        Student stu = findByIdAndTeacherId(id, teacherId);
        studentRepository.delete(stu);
    }

    @Transactional
    public Student adoptPet(String studentId, String teacherId, Integer petId, String petName, String petIcon) {
        Student stu = findByIdAndTeacherId(studentId, teacherId);
        
        // 如果已有宠物，需要消耗宠物更换卡
        if (stu.getPetId() != null) {
            int cards = stu.getPetChangeCards() != null ? stu.getPetChangeCards() : 0;
            if (cards <= 0) {
                throw new RuntimeException("请先购买宠物更换卡");
            }
            stu.setPetChangeCards(cards - 1);
        }
        
        stu.setPetId(petId);
        stu.setPetName(petName);
        stu.setPetIcon(petIcon);
        return studentRepository.save(stu);
    }

    @Transactional
    public List<Student> assignPetsRandomly(String teacherId) {
        List<Student> students = studentRepository.findByTeacherIdOrderByCreatedAtAsc(teacherId);
        int[][] petIds = {{1, 2}, {3, 4}, {5, 6}, {7, 8}, {9, 10}};
        for (int i = 0; i < students.size(); i++) {
            students.get(i).setPetId(petIds[i % petIds.length][i % 2]);
        }
        return studentRepository.saveAll(students);
    }

    @Transactional
    public Student applyScore(String studentId, String teacherId, String scoreItemId) {
        Student stu = findByIdAndTeacherId(studentId, teacherId);
        Optional<ScoreItem> item = scoreItemRepository.findById(scoreItemId);
        int delta = item.map(ScoreItem::getPoint).orElse(1);
        stu.setFood(Math.max(0, stu.getFood() + delta));
        studentRepository.save(stu);

        // 写入历史记录
        ScoreHistory history = new ScoreHistory(
            stu.getId(), stu.getName(),
            item.map(ScoreItem::getName).orElse("手动加分"),
            item.map(ScoreItem::getIcon).orElse("✏️"),
            delta, teacherId
        );
        scoreHistoryRepository.save(history);

        return stu;
    }

    public List<Student> getLeaderboard(String teacherId) {
        return studentRepository.findByTeacherIdOrderByFoodDesc(teacherId);
    }

    // 总积分排名：历史所有加分 - 历史所有扣分（不包括道具消耗）
    public List<Map<String, Object>> getTotalScoreLeaderboard(String teacherId) {
        List<Student> students = studentRepository.findByTeacherIdOrderByCreatedAtAsc(teacherId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Student stu : students) {
            // 计算该学生的总积分：所有未撤销的历史记录的point之和
            List<ScoreHistory> histories = scoreHistoryRepository.findByStudentIdAndRevokedFalse(stu.getId());
            int totalScore = histories.stream().mapToInt(h -> h.getPoint() != null ? h.getPoint() : 0).sum();
            
            Map<String, Object> item = new HashMap<>();
            item.put("id", stu.getId());
            item.put("name", stu.getName());
            item.put("petIcon", stu.getPetIcon());
            item.put("petName", stu.getPetName());
            item.put("food", stu.getFood()); // 当前积分
            item.put("totalScore", totalScore); // 总积分
            item.put("equippedItems", stu.getEquippedItems());
            result.add(item);
        }
        
        // 按总积分降序排序
        result.sort((a, b) -> Integer.compare((Integer) b.get("totalScore"), (Integer) a.get("totalScore")));
        return result;
    }

    private Student findByIdAndTeacherId(String id, String teacherId) {
        return studentRepository.findByIdAndTeacherId(id, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("未找到该学生"));
    }

    // ============== 装备装饰 ==============
    @Transactional
    public Student equipItem(String studentId, String teacherId, String itemId) {
        Student stu = findByIdAndTeacherId(studentId, teacherId);
        // 验证商品存在且属于该教师
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!item.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("无权限");
        }
        // 解析当前装备列表
        List<String> equipped = parseEquippedList(stu.getEquippedItems());
        if (!equipped.contains(itemId)) {
            equipped.add(itemId);
            try { stu.setEquippedItems(objectMapper.writeValueAsString(equipped)); } 
            catch (JsonProcessingException e) { stu.setEquippedItems("[]"); }
        }
        return studentRepository.save(stu);
    }

    @Transactional
    public Student unequipItem(String studentId, String teacherId, String itemId) {
        Student stu = findByIdAndTeacherId(studentId, teacherId);
        List<String> equipped = parseEquippedList(stu.getEquippedItems());
        equipped.remove(itemId);
        try { stu.setEquippedItems(objectMapper.writeValueAsString(equipped)); }
        catch (JsonProcessingException e) { stu.setEquippedItems("[]"); }
        return studentRepository.save(stu);
    }

    public List<String> parseEquippedList(String json) {
        try {
            if (json == null || json.isBlank()) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getPetLibrary() {
        return List.of(
            Map.of("id", 1, "icon", "🐱", "name", "小橘猫"),
            Map.of("id", 2, "icon", "🐶", "name", "柴犬"),
            Map.of("id", 3, "icon", "🐰", "name", "垂耳兔"),
            Map.of("id", 4, "icon", "🐹", "name", "金丝熊"),
            Map.of("id", 5, "icon", "🦊", "name", "小狐狸"),
            Map.of("id", 6, "icon", "🐼", "name", "熊猫"),
            Map.of("id", 7, "icon", "🦁", "name", "小狮子"),
            Map.of("id", 8, "icon", "🐯", "name", "小老虎"),
            Map.of("id", 9, "icon", "🐨", "name", "考拉"),
            Map.of("id", 10, "icon", "🐮", "name", "奶牛")
        );
    }
}