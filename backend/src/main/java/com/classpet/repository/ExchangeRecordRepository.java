package com.classpet.repository;

import com.classpet.entity.ExchangeRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExchangeRecordRepository extends JpaRepository<ExchangeRecord, String> {
    List<ExchangeRecord> findByTeacherIdOrderByCreatedAtDesc(String teacherId);
    List<ExchangeRecord> findByStudentIdOrderByCreatedAtDesc(String studentId);

    // ===== 游标分页 =====
    @Query("SELECT e FROM ExchangeRecord e "
         + "WHERE e.teacherId = :tid "
         + "AND (:cursorTime IS NULL OR (e.createdAt < :cursorTime OR (e.createdAt = :cursorTime AND e.id < :cursorId))) "
         + "ORDER BY e.createdAt DESC, e.id DESC")
    List<ExchangeRecord> findTeacherPage(@Param("tid") String tid,
                                         @Param("cursorTime") LocalDateTime cursorTime,
                                         @Param("cursorId") String cursorId,
                                         Pageable page);

    @Query("SELECT e FROM ExchangeRecord e "
         + "WHERE e.studentId = :sid "
         + "AND (:cursorTime IS NULL OR (e.createdAt < :cursorTime OR (e.createdAt = :cursorTime AND e.id < :cursorId))) "
         + "ORDER BY e.createdAt DESC, e.id DESC")
    List<ExchangeRecord> findStudentPage(@Param("sid") String sid,
                                         @Param("cursorTime") LocalDateTime cursorTime,
                                         @Param("cursorId") String cursorId,
                                         Pageable page);
}