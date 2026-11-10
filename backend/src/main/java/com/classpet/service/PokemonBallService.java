package com.classpet.service;

import com.classpet.entity.ClassroomPokemonPool;
import com.classpet.entity.PokemonBall;
import com.classpet.entity.Student;
import com.classpet.repository.ClassroomPokemonPoolRepository;
import com.classpet.repository.PokemonBallRepository;
import com.classpet.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 精灵球发放 / 消耗。
 *
 * <p>关键规则：<b>发球时把当时的班级池快照写进球里</b>，开球用快照。
 * 这样"上月发的球、本月换了池子"不会被新池子影响。</p>
 *
 * <p>{@code students.pokemon_bells} 继续作为未开球数量使用（前端展示、快速判断），
 * 与 pokemon_balls 里的未开球行数保持同步。</p>
 */
@Service
public class PokemonBallService {

    private static final Logger logger = LoggerFactory.getLogger(PokemonBallService.class);

    private final PokemonBallRepository ballRepo;
    private final ClassroomPokemonPoolRepository poolRepo;
    private final StudentRepository studentRepo;

    public PokemonBallService(PokemonBallRepository ballRepo,
                              ClassroomPokemonPoolRepository poolRepo,
                              StudentRepository studentRepo) {
        this.ballRepo = ballRepo;
        this.poolRepo = poolRepo;
        this.studentRepo = studentRepo;
    }

    // ---------------- 池子快照 ----------------

    /**
     * 取某个班级当前池子的 JSON 快照。
     *
     * <p>老数据的池子是按教师存的（classroom_id = teacherId），迁移完成前这里做一次兜底，
     * 避免升级期间出现"有池子却抽默认池"。</p>
     */
    public String snapshotOfClass(String classId, String teacherId) {
        ClassroomPokemonPool pool = null;
        if (classId != null && !classId.isBlank()) {
            pool = poolRepo.findByClassroomId(classId).orElse(null);
        }
        if (pool == null && teacherId != null && !teacherId.isBlank()) {
            pool = poolRepo.findByClassroomId(teacherId).orElse(null);
        }
        if (pool == null || pool.getPokedexEntries() == null) return "[]";
        String json = pool.getPokedexEntries().trim();
        return (json.isEmpty() || "[]".equals(json)) ? "[]" : json;
    }

    public String snapshotOfStudent(Student student) {
        return snapshotOfClass(student.getClassId(), student.getTeacherId());
    }

    // ---------------- 发放 ----------------

    /**
     * 只补一条球记录（不改计数）。
     * 用于"小卖部买球"这类调用方已经自己把计数 +1 的场景。
     */
    @Transactional
    public PokemonBall attach(Student student, String source) {
        return ballRepo.save(new PokemonBall(
                student.getId(), student.getTeacherId(), student.getClassId(),
                snapshotOfStudent(student), source));
    }

    /** 给单个学生发 1 个球，快照取该生当前班级的池子 */
    @Transactional
    public PokemonBall grantOne(Student student, String source, String snapshot) {
        PokemonBall ball = ballRepo.save(new PokemonBall(
                student.getId(), student.getTeacherId(), student.getClassId(),
                snapshot != null ? snapshot : snapshotOfStudent(student), source));

        int current = student.getPokemonBalls() != null ? student.getPokemonBalls() : 0;
        student.setPokemonBalls(current + 1);
        studentRepo.save(student);
        return ball;
    }

    /** 给一个班级的所有学生各发 1 个球（同一份快照） */
    @Transactional
    public int grantToClass(String classId, String teacherId, String source) {
        List<Student> students = studentRepo.findByClassIdOrderByCreatedAtAsc(classId);
        String snapshot = snapshotOfClass(classId, teacherId);
        for (Student s : students) {
            grantOne(s, source, snapshot);
        }
        return students.size();
    }

    /**
     * 给某教师名下所有学生发球。
     * 按班级分组，每个班只解析一次池子快照。
     */
    @Transactional
    public int grantToTeacher(String teacherId, String source) {
        List<Student> students = studentRepo.findByTeacherIdOrderByCreatedAtAsc(teacherId);
        Map<String, String> snapshotCache = new HashMap<>();
        for (Student s : students) {
            String key = s.getClassId() == null ? "" : s.getClassId();
            String snapshot = snapshotCache.computeIfAbsent(key, k -> snapshotOfStudent(s));
            grantOne(s, source, snapshot);
        }
        return students.size();
    }

    /** 给全体学生发球（每月定时任务用） */
    @Transactional
    public int grantToAllStudents(String source) {
        List<Student> students = studentRepo.findAll();
        Map<String, String> snapshotCache = new HashMap<>();
        for (Student s : students) {
            String key = s.getClassId() == null ? "" : s.getClassId();
            String snapshot = snapshotCache.computeIfAbsent(key, k -> snapshotOfStudent(s));
            grantOne(s, source, snapshot);
        }
        logger.info("精灵球发放完成：{} 位学生，来源 {}", students.size(), source);
        return students.size();
    }

    // ---------------- 消耗 ----------------

    /** 取最早的一颗未开球；历史遗留（只有计数、没有球记录）返回 empty */
    public Optional<PokemonBall> peekOldestUnopened(String studentId) {
        return ballRepo.findFirstByStudentIdAndOpenedAtIsNullOrderByGrantedAtAscIdAsc(studentId);
    }

    @Transactional
    public void markOpened(PokemonBall ball, Integer pokedexId) {
        ball.setOpenedAt(LocalDateTime.now());
        ball.setOpenedPokedexId(pokedexId);
        ballRepo.save(ball);
    }
}
