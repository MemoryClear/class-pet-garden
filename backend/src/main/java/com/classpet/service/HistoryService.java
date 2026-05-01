package com.classpet.service;

import com.classpet.entity.ScoreHistory;
import com.classpet.entity.Student;
import com.classpet.repository.ScoreHistoryRepository;
import com.classpet.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistoryService {

    @Autowired private ScoreHistoryRepository historyRepo;
    @Autowired private StudentRepository studentRepo;

    public List<ScoreHistory> getHistory(String teacherId, String studentId,
                                   LocalDate from, LocalDate to) {
        if (studentId != null) {
            return historyRepo.findByTeacherIdAndStudentIdOrderByCreatedAtDesc(teacherId, studentId);
        }
        if (from != null && to != null) {
            return historyRepo.findByTeacherIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                    teacherId,
                    from.atStartOfDay(),
                    to.plusDays(1).atStartOfDay());
        }
        return historyRepo.findByTeacherIdOrderByCreatedAtDesc(teacherId);
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