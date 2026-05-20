package com.classpet.repository;

import com.classpet.entity.ReadingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingRecordRepository extends JpaRepository<ReadingRecord, String> {
    
    Optional<ReadingRecord> findByStudentIdAndActivityTypeAndItemIdAndRecordDate(
        String studentId, String activityType, String itemId, LocalDate recordDate);
    
    List<ReadingRecord> findByStudentIdAndActivityTypeAndRecordDate(
        String studentId, String activityType, LocalDate recordDate);
    
    @Query("SELECT COALESCE(SUM(r.score), 0) FROM ReadingRecord r " +
           "WHERE r.studentId = :studentId AND r.activityType = :activityType " +
           "AND r.itemId = :itemId AND r.recordDate = :recordDate")
    Integer sumScoreByStudentAndActivityAndItemAndDate(
        @Param("studentId") String studentId,
        @Param("activityType") String activityType,
        @Param("itemId") String itemId,
        @Param("recordDate") LocalDate recordDate);
    
    @Query("SELECT COALESCE(SUM(r.score), 0) FROM ReadingRecord r " +
           "WHERE r.studentId = :studentId AND r.activityType = :activityType " +
           "AND r.recordDate = :recordDate")
    Integer sumScoreByStudentAndActivityAndDate(
        @Param("studentId") String studentId,
        @Param("activityType") String activityType,
        @Param("recordDate") LocalDate recordDate);
}
