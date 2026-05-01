package com.classpet.service;

import com.classpet.entity.ScoreItem;
import com.classpet.repository.ScoreItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoreItemService {

    @Autowired private ScoreItemRepository scoreItemRepo;

    public List<ScoreItem> getScoreItems(String teacherId) {
        return scoreItemRepo.findByTeacherIdOrderByCreatedAtAsc(teacherId);
    }

    public ScoreItem createScoreItem(String icon, String name, Integer point, String teacherId) {
        ScoreItem item = new ScoreItem(icon, name, point, teacherId);
        return scoreItemRepo.save(item);
    }

    public ScoreItem initializeDefaults(String teacherId) {
        List<ScoreItem> existing = scoreItemRepo.findByTeacherIdOrderByCreatedAtAsc(teacherId);
        if (!existing.isEmpty()) return existing.get(0);

        createScoreItem("📖", "早读打卡", 1, teacherId);
        createScoreItem("💡", "答对问题", 2, teacherId);
        createScoreItem("📘", "作业优秀", 3, teacherId);
        createScoreItem("🎤", "完成背诵", 2, teacherId);
        createScoreItem("🌱", "进步明显", 3, teacherId);
        createScoreItem("⏰", "迟到", -1, teacherId);
        createScoreItem("📱", "玩手机", -2, teacherId);
        createScoreItem("😴", "打瞌睡", -1, teacherId);
        return scoreItemRepo.findByTeacherIdOrderByCreatedAtAsc(teacherId).get(0);
    }

    public void deleteScoreItem(String id, String teacherId) {
        scoreItemRepo.findById(id)
                .filter(i -> i.getTeacherId().equals(teacherId))
                .ifPresent(scoreItemRepo::delete);
    }
}