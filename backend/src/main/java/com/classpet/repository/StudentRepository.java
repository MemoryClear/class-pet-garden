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
    Optional<Student> findByStudentNo(String studentNo);

    // ===== 多班级：按班级过滤 =====
    List<Student> findByClassIdOrderByCreatedAtAsc(String classId);
    List<Student> findByClassIdOrderByFoodDesc(String classId);
    List<Student> findByClassIdAndPetIdIsNull(String classId);
    long countByClassId(String classId);
    boolean existsByClassIdAndName(String classId, String name);
    java.util.Optional<Student> findByClassIdAndStudentNo(String classId, String studentNo);
    Optional<Student> findByIdAndClassId(String id, String classId);
    java.util.Optional<Student> findByTeacherIdAndStudentNo(String teacherId, String studentNo);
}