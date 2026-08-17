package com.classpet.service;

import com.classpet.entity.ScoreHistory;
import com.classpet.entity.Student;
import com.classpet.repository.ScoreHistoryRepository;
import com.classpet.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistoryService {

    @Autowired private ScoreHistoryRepository historyRepo;
    @Autowired private StudentRepository studentRepo;

    /**
     * 游标分页：响应包含 items / hasMore / nextCursor
     * - teacherId 必填
     * - studentId/from/to 可选过滤（兼容旧版全量接口）
     * - cursorTime + cursorId 决定从哪条之后继续（首次为 null）
     * - limit 上限 100
     */
    public Map<String, Object> getHistoryPage(String teacherId,
                                              String studentId,
                                              LocalDate from, LocalDate to,
                                              LocalDateTime cursorTime, String cursorId,
                                              int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        // 多取1条用于判断 hasMore
        PageRequest pageReq = PageRequest.of(0, safeLimit + 1);
        List<ScoreHistory> rows;
        if (studentId != null && !studentId.isEmpty()) {
            rows = historyRepo.findTeacherStudentPage(teacherId, studentId, cursorTime, cursorId, pageReq);
        } else if (from != null && to != null) {
            LocalDateTime fromDt = from.atStartOfDay();
            LocalDateTime toDt = to.plusDays(1).atStartOfDay();
            rows = historyRepo.findTeacherBetweenPage(teacherId, fromDt, toDt, cursorTime, cursorId, pageReq);
        } else {
            rows = historyRepo.findTeacherPage(teacherId, cursorTime, cursorId, pageReq);
        }
        boolean hasMore = rows.size() > safeLimit;
        if (hasMore) rows = rows.subList(0, safeLimit);

        Map<String, Object> resp = new HashMap<>();
        resp.put("items", rows);
        resp.put("hasMore", hasMore);
        if (hasMore && !rows.isEmpty()) {
            ScoreHistory tail = rows.get(rows.size() - 1);
            Map<String, Object> cursor = new HashMap<>();
            cursor.put("createdAt", tail.getCreatedAt());
            cursor.put("id", tail.getId());
            resp.put("nextCursor", cursor);
        } else {
            resp.put("nextCursor", null);
        }
        return resp;
    }

    @Transactional
    public ScoreHistory revokeScore(String historyId, String teacherId) {
        ScoreHistory record = historyRepo.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        if (!record.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("无权限操作");
        }
        if (Boolean.TRUE.equals(record.getRevoked())) {
            throw new IllegalArgumentException("该记录已被撤销");
        }

        // 反向调整学生积分
        Student student = studentRepo.findById(record.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        student.setFood(Math.max(0, student.getFood() - record.getPoint()));
        studentRepo.save(student);

        // 标记撤销
        record.setRevoked(true);
        record.setRevokedAt(LocalDateTime.now());
        return historyRepo.save(record);
    }
}