package com.classpet.repository;

import com.classpet.entity.ScoreItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScoreItemRepository extends JpaRepository<ScoreItem, String> {
    List<ScoreItem> findByTeacherIdOrderByCreatedAtAsc(String teacherId);
}