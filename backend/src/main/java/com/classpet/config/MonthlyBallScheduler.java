package com.classpet.config;

import com.classpet.entity.Student;
import com.classpet.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private StudentRepository studentRepository;
    
    /**
     * 每月1日 00:00 发放精灵球
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void distributeMonthlyBalls() {
        logger.info("开始发放每月精灵球...");
        
        List<Student> students = studentRepository.findAll();
        int count = 0;
        
        for (Student student : students) {
            int currentBalls = student.getPokemonBalls() != null ? student.getPokemonBalls() : 0;
            student.setPokemonBalls(currentBalls + 1);
            studentRepository.save(student);
            count++;
        }
        
        logger.info("精灵球发放完成，共发放 {} 个学生", count);
    }
    
    /**
     * 手动触发发放（用于测试或补发）
     */
    @Transactional
    public int manualDistribute() {
        List<Student> students = studentRepository.findAll();
        int count = 0;
        
        for (Student student : students) {
            int currentBalls = student.getPokemonBalls() != null ? student.getPokemonBalls() : 0;
            student.setPokemonBalls(currentBalls + 1);
            studentRepository.save(student);
            count++;
        }
        
        return count;
    }
}
