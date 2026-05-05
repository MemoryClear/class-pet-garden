package com.classpet.repository;

import com.classpet.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassroomRepository extends JpaRepository<Classroom, String> {
    List<Classroom> findByTeacherIdOrderByCreatedAtDesc(String teacherId);
    void deleteByTeacherId(String teacherId);
}