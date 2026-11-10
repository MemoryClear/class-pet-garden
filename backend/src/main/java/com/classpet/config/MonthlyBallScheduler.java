package com.classpet.config;

import com.classpet.service.PokemonBallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.classpet.entity.PokemonBall;

/**
 * 每月精灵球发放定时任务
 * 
 * 发放规则：
 * - 每月1日 00:00 自动执行
 * - 所有学生获得 1 个精灵球
 * - 精灵球可叠加（未领取的累积到下个月）
 */
@Component
public class MonthlyBallScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(MonthlyBallScheduler.class);
    
    @Autowired
    private PokemonBallService pokemonBallService;
    
    /**
     * 每月1日 00:00 发放精灵球
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void distributeMonthlyBalls() {
        logger.info("开始发放每月精灵球...");
        int count = pokemonBallService.grantToAllStudents(PokemonBall.SOURCE_MONTHLY);
        logger.info("精灵球发放完成，共发放 {} 个学生", count);
    }
    
    /**
     * 手动触发发放（用于测试或补发）。
     *
     * <p>只发给指定教师名下的学生 —— 之前用的是 findAll()，
     * 多教师部署时会连别人的学生一起发球。</p>
     */
    @Transactional
    public int manualDistribute(String teacherId, String classId) {
        if (teacherId == null || teacherId.isBlank()) {
            throw new IllegalArgumentException("缺少教师信息，无法发放精灵球");
        }
        if (classId != null && !classId.isBlank()) {
            return pokemonBallService.grantToClass(classId, teacherId, PokemonBall.SOURCE_MANUAL);
        }
        return pokemonBallService.grantToTeacher(teacherId, PokemonBall.SOURCE_MANUAL);
    }
}
