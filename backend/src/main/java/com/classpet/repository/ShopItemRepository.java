package com.classpet.repository;

import com.classpet.entity.ShopItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShopItemRepository extends JpaRepository<ShopItem, String> {
    List<ShopItem> findByTeacherIdOrderByCreatedAtDesc(String teacherId);
    void deleteByIdAndTeacherId(String id, String teacherId);
}