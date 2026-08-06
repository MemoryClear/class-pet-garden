package com.classpet.migration;

import com.classpet.entity.Student;
import com.classpet.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 老学生密码迁移器。
 *
 * <p>职责：
 * <ol>
 *   <li>检查 students 表是否存在 password_hash / must_change_password 列，
 *       不存在则手工 ALTER TABLE 补齐（SQLite 的 ddl-auto=update 对 NOT NULL 列无能为力）。</li>
 *   <li>为没有密码的老学生设置默认密码=学号，并强制改密。</li>
 * </ol>
 *
 * <p>触发时机：ApplicationRunner 在 Spring Boot 应用上下文刷新完成、Servlet 容器启动之前执行。
 * 此时 Hibernate ddl-auto=update 已完成建列，所以 JdbcTemplate 能直接拿到新列。
 * 用 {@code @Order(Ordered.HIGHEST_PRECEDENCE)} 保证在其他 Runner 之前跑（以防它们读 students 表）。</p>
 *
 * <p>幂等性：每次启动都会跑，但：
 * <ul>
 *   <li>列存在性检查（{@code pragma_table_info}）确保 ALTER 不重复执行。</li>
 *   <li>{@code s.getPasswordHash() == null} 判断确保已经迁移过的学生不会被覆盖。</li>
 * </ul>
 */
@Component
@Order(Integer.MIN_VALUE + 1)
public class StudentPasswordMigration implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StudentPasswordMigration.class);

    private final JdbcTemplate jdbcTemplate;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentPasswordMigration(JdbcTemplate jdbcTemplate,
                                    StudentRepository studentRepository,
                                    PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureColumnExists("password_hash",
                "ALTER TABLE students ADD COLUMN password_hash VARCHAR(255)");
        ensureColumnExists("must_change_password",
                "ALTER TABLE students ADD COLUMN must_change_password BOOLEAN DEFAULT 0");

        int renumbered = renumberLegacyStudentNos();
        int migrated = migrateLegacyStudents();
        if (renumbered > 0) {
            logger.info("学号前缀迁移：共为 {} 个老学生补齐班级前缀（不更改密码）", renumbered);
        } else {
            logger.info("学号前缀迁移：无需迁移（所有学生均已含班级前缀）");
        }
        if (migrated > 0) {
            logger.info("学生密码迁移：共迁移 {} 个老学生（默认密码=学号，需强制改密）", migrated);
        } else {
            logger.info("学生密码迁移：无需迁移（所有学生均已有密码）");
        }
    }

    /**
     * 检查列是否存在，不存在则 ALTER TABLE。
     * SQLite 的 {@code pragma_table_info} 返回的是单列视图（列名/类型...），
     * 我们只需要 name 列做存在性判断。
     */
    private void ensureColumnExists(String columnName, String alterSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pragma_table_info('students') WHERE name = ?",
                    Integer.class, columnName);
            if (count == null || count == 0) {
                jdbcTemplate.execute(alterSql);
                logger.info("学生密码迁移：已添加 {} 列", columnName);
            }
        } catch (Exception e) {
            // 列已存在或其他兼容问题：忽略即可
            logger.debug("学生密码迁移：列 {} 检查跳过（{}）", columnName, e.getMessage());
        }
    }

    /**
     * 迁移老学生：没密码的统一设默认=学号，强制改密。
     * 幂等：已迁移过的学生 passwordHash 非空，跳过。
     */
    private int migrateLegacyStudents() {
        List<Student> all = studentRepository.findAll();
        int migrated = 0;
        for (Student s : all) {
            if (s.getPasswordHash() == null || s.getPasswordHash().isEmpty()) {
                String defaultPwd = s.getStudentNo() != null ? s.getStudentNo() : ("S" + s.getId());
                s.setPasswordHash(passwordEncoder.encode(defaultPwd));
                s.setMustChangePassword(true);
                studentRepository.save(s);
                migrated++;
            }
        }
        return migrated;
    }

    /**
     * 给老学生补齐班级前缀。原始设计是全局 S0001 起编号，多个老师会撞。
     * 新方案：{4位十六进制前缀}-S0001，例如 E8DC-S0001。
     *
     * <p>计数器起点 = 该老师名下已有迁移学号的最大序号 + 1，避免撞 UNIQUE。
     * 例如老师名下已有 E8DC-S0001 / E8DC-S0002，则非法 studentNo="1" 会改名为 E8DC-S0003。</p>
     */
    private int renumberLegacyStudentNos() {
        List<Student> all = studentRepository.findAll();
        // 第一遍：按 teacherId 算已有迁移学号的最大序号
        Map<String, Integer> maxCounter = new java.util.HashMap<>();
        java.util.regex.Pattern migratedPattern =
                java.util.regex.Pattern.compile("^[A-Z0-9]{4}-S\\d{4}$");
        for (Student s : all) {
            String sno = s.getStudentNo();
            if (sno == null) continue;
            if (!migratedPattern.matcher(sno).matches()) continue;
            int dashIdx = sno.indexOf('-');
            int seq;
            try {
                seq = Integer.parseInt(sno.substring(dashIdx + 2));
            } catch (NumberFormatException e) {
                continue;
            }
            maxCounter.merge(s.getTeacherId(), seq, Math::max);
        }
        // 第二遍：给无迁移学号的补前缀（非法格式也改）
        Map<String, Integer> counter = new java.util.HashMap<>();
        int renumbered = 0;
        for (Student s : all) {
            String sno = s.getStudentNo();
            if (sno == null) continue;
            if (sno.contains("-")) continue;  // 新格式已迁移
            String tid = s.getTeacherId();
            int next = counter.getOrDefault(tid, maxCounter.getOrDefault(tid, 0)) + 1;
            String prefix = teacherPrefix(tid);
            String newSno = String.format("%s-S%04d", prefix, next);
            s.setStudentNo(newSno);
            studentRepository.save(s);
            renumbered++;
            counter.put(tid, next);
            logger.info("学号迁移：{} → {} (teacherId={})", sno, newSno, tid);
        }
        return renumbered;
    }

    private String teacherPrefix(String teacherId) {
        if (teacherId == null || teacherId.length() < 4) return "T000";
        return teacherId.replace("-", "").substring(0, 4).toUpperCase();
    }

    /** 给运维端点用的状态查询 */
    public MigrationStatus getStatus() {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students", Integer.class);
        Integer withPwd = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students WHERE password_hash IS NOT NULL AND password_hash != ''",
                Integer.class);
        Integer mustChange = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students WHERE must_change_password = 1",
                Integer.class);
        return new MigrationStatus(
                total == null ? 0 : total,
                withPwd == null ? 0 : withPwd,
                mustChange == null ? 0 : mustChange);
    }

    public record MigrationStatus(int totalStudents, int withPassword, int mustChangePassword) {}
}