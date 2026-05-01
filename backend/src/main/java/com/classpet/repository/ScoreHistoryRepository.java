package com.classpet.repository;

import com.classpet.entity.ScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScoreHistoryRepository extends JpaRepository<ScoreHistory, String> {
    List<ScoreHistory> findByTeacherIdOrderByCreatedAtDesc(String teacherId);
    List<ScoreHistory> findByTeacherIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        String teacherId, LocalDateTime from, LocalDateTime to);
    List<ScoreHistory> findByStudentIdOrderByCreatedAtDesc(String studentId);
    List<ScoreHistory> findByTeacherIdAndStudentIdOrderByCreatedAtDesc(String teacherId, String studentId);
    List<ScoreHistory> findByStudentIdAndRevokedFalse(String studentId);
}