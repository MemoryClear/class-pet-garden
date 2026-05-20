package com.classpet.repository;

import com.classpet.entity.ReadingSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingSettingRepository extends JpaRepository<ReadingSetting, String> {
    
    Optional<ReadingSetting> findByTeacherIdAndActivityType(String teacherId, String activityType);
    
    List<ReadingSetting> findByTeacherId(String teacherId);
}
