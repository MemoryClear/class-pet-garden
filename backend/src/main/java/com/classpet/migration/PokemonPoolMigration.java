package com.classpet.migration;

import com.classpet.entity.ClassroomPokemonPool;
import com.classpet.entity.PokemonBall;
import com.classpet.entity.SchoolClass;
import com.classpet.entity.Student;
import com.classpet.repository.ClassroomPokemonPoolRepository;
import com.classpet.repository.PokemonBallRepository;
import com.classpet.repository.SchoolClassRepository;
import com.classpet.repository.StudentRepository;
import com.classpet.service.PokemonBallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 宝可梦池 / 精灵球迁移（多班级 + 球快照）。
 *
 * <p>两步，都是幂等的：</p>
 * <ol>
 *   <li><b>池子改挂到班级</b>：老数据的 classroom_pokemon_pool.classroom_id 存的是教师 id，
 *       复制到该教师的每个班级（已有的班跳过），然后删掉教师级那条。</li>
 *   <li><b>补球记录</b>：以前精灵球只是 students.pokemon_bells 上的数字。
 *       这里给"有剩余球但没有球记录"的学生补出对应条数的 PokemonBall，
 *       快照取当前班级池子（历史池子已经无法还原，只能从升级这一刻起锁定）。</li>
 * </ol>
 */
@Component
@Order(Integer.MIN_VALUE + 3)
public class PokemonPoolMigration implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PokemonPoolMigration.class);

    private final ClassroomPokemonPoolRepository poolRepo;
    private final SchoolClassRepository classRepo;
    private final StudentRepository studentRepo;
    private final PokemonBallRepository ballRepo;
    private final PokemonBallService ballService;

    public PokemonPoolMigration(ClassroomPokemonPoolRepository poolRepo,
                                SchoolClassRepository classRepo,
                                StudentRepository studentRepo,
                                PokemonBallRepository ballRepo,
                                PokemonBallService ballService) {
        this.poolRepo = poolRepo;
        this.classRepo = classRepo;
        this.studentRepo = studentRepo;
        this.ballRepo = ballRepo;
        this.ballService = ballService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migratePoolsToClass();
        backfillBalls();
    }

    // ---------------- 1. 池子按班级 ----------------

    private void migratePoolsToClass() {
        List<ClassroomPokemonPool> pools = poolRepo.findAll();
        int moved = 0;
        int dropped = 0;

        for (ClassroomPokemonPool pool : pools) {
            String key = pool.getClassroomId();
            if (key == null || key.isBlank()) continue;

            // 已经是班级 id → 跳过
            if (classRepo.findById(key).isPresent()) continue;

            // 教师 id → 复制到该教师的每个班级（没有池子的班才复制），
            // 然后删掉教师级那条。这样多班级下每个班初始看到的池子和升级前一致，
            // 老师之后可以各自再改。
            List<SchoolClass> classes = classRepo.findByTeacherIdOrderBySortOrderAscCreatedAtAsc(key);
            if (classes.isEmpty()) continue;

            for (SchoolClass c : classes) {
                boolean exists = poolRepo.findByClassroomId(c.getId()).isPresent();
                if (!exists) {
                    ClassroomPokemonPool copy = new ClassroomPokemonPool(c.getId());
                    copy.setPokedexEntries(pool.getPokedexEntries());
                    poolRepo.save(copy);
                    moved++;
                }
            }
            poolRepo.delete(pool);
            dropped++;
        }

        if (moved > 0 || dropped > 0) {
            logger.info("宝可梦池迁移：复制出 {} 条班级级池子，清理 {} 条教师级历史记录", moved, dropped);
        }
    }

    // ---------------- 2. 老精灵球补记录 ----------------

    private void backfillBalls() {
        List<Student> students = studentRepo.findAll();
        int created = 0;

        for (Student s : students) {
            int counter = s.getPokemonBalls() != null ? s.getPokemonBalls() : 0;
            if (counter <= 0) continue;

            long unopened = ballRepo.countByStudentIdAndOpenedAtIsNull(s.getId());
            if (unopened >= counter) continue;

            int missing = (int) (counter - unopened);
            String snapshot = ballService.snapshotOfStudent(s);
            for (int i = 0; i < missing; i++) {
                ballRepo.save(new PokemonBall(
                        s.getId(), s.getTeacherId(), s.getClassId(),
                        snapshot, PokemonBall.SOURCE_MIGRATED));
                created++;
            }
        }

        if (created > 0) {
            logger.info("精灵球迁移：为历史剩余球补建 {} 条记录（快照取当前班级池子）", created);
        }
    }
}
