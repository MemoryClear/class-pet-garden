package com.classpet.repository;

import com.classpet.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, String> {
    Optional<Teacher> findByUsername(String username);
    boolean existsByUsername(String username);
}