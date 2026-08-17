package com.classpet.repository;

import com.classpet.entity.ScoreHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // ===== 游标分页 =====
    // 教师范围，按 (createdAt DESC, id DESC) 排序，复合游标 (cursorTime, cursorId)
    @Query("SELECT s FROM ScoreHistory s "
         + "WHERE s.teacherId = :tid "
         + "AND (:cursorTime IS NULL OR (s.createdAt < :cursorTime OR (s.createdAt = :cursorTime AND s.id < :cursorId))) "
         + "ORDER BY s.createdAt DESC, s.id DESC")
    List<ScoreHistory> findTeacherPage(@Param("tid") String tid,
                                       @Param("cursorTime") LocalDateTime cursorTime,
                                       @Param("cursorId") String cursorId,
                                       Pageable page);

    @Query("SELECT s FROM ScoreHistory s "
         + "WHERE s.teacherId = :tid AND s.studentId = :sid "
         + "AND (:cursorTime IS NULL OR (s.createdAt < :cursorTime OR (s.createdAt = :cursorTime AND s.id < :cursorId))) "
         + "ORDER BY s.createdAt DESC, s.id DESC")
    List<ScoreHistory> findTeacherStudentPage(@Param("tid") String tid,
                                              @Param("sid") String sid,
                                              @Param("cursorTime") LocalDateTime cursorTime,
                                              @Param("cursorId") String cursorId,
                                              Pageable page);

    @Query("SELECT s FROM ScoreHistory s "
         + "WHERE s.teacherId = :tid AND s.createdAt >= :from AND s.createdAt < :to "
         + "AND (:cursorTime IS NULL OR (s.createdAt < :cursorTime OR (s.createdAt = :cursorTime AND s.id < :cursorId))) "
         + "ORDER BY s.createdAt DESC, s.id DESC")
    List<ScoreHistory> findTeacherBetweenPage(@Param("tid") String tid,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to,
                                              @Param("cursorTime") LocalDateTime cursorTime,
                                              @Param("cursorId") String cursorId,
                                              Pageable page);

    // 学生范围
    @Query("SELECT s FROM ScoreHistory s "
         + "WHERE s.studentId = :sid "
         + "AND (:cursorTime IS NULL OR (s.createdAt < :cursorTime OR (s.createdAt = :cursorTime AND s.id < :cursorId))) "
         + "ORDER BY s.createdAt DESC, s.id DESC")
    List<ScoreHistory> findStudentPage(@Param("sid") String sid,
                                       @Param("cursorTime") LocalDateTime cursorTime,
                                       @Param("cursorId") String cursorId,
                                       Pageable page);
}