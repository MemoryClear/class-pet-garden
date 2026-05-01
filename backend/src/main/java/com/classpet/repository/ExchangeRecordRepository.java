package com.classpet.repository;

import com.classpet.entity.ExchangeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExchangeRecordRepository extends JpaRepository<ExchangeRecord, String> {
    List<ExchangeRecord> findByTeacherIdOrderByCreatedAtDesc(String teacherId);
    List<ExchangeRecord> findByStudentIdOrderByCreatedAtDesc(String studentId);
}