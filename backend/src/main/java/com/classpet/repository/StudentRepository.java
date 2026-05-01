package com.classpet.repository;

import com.classpet.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    List<Student> findByTeacherIdOrderByCreatedAtAsc(String teacherId);
    List<Student> findByTeacherIdAndPetIdIsNull(String teacherId);
    boolean existsByTeacherIdAndName(String teacherId, String name);
    boolean existsByTeacherIdAndNameAndIdNot(String teacherId, String name, String id);
    java.util.Optional<Student> findByIdAndTeacherId(String id, String teacherId);
    List<Student> findByTeacherIdOrderByFoodDesc(String teacherId);
}