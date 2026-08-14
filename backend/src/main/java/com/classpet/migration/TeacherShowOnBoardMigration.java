package com.classpet.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 为 teachers 表补充 show_on_board 列。
 *
 * <p>Hibernate ddl-auto=update 对 SQLite 添加 NOT NULL 列偶尔会静默跳过，
 * 这里用 JdbcTemplate 手工确保列存在（带 DEFAULT 0）。
 *
 * <p>该字段控制教师是否将本班加入"公开看板"（无需登录可看排行榜与学生详情）。
 */
@Component
@Order(Integer.MIN_VALUE + 1)
public class TeacherShowOnBoardMigration implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TeacherShowOnBoardMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public TeacherShowOnBoardMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureColumnExists("show_on_board",
                "ALTER TABLE teachers ADD COLUMN show_on_board BOOLEAN DEFAULT 0");
    }

    private void ensureColumnExists(String columnName, String alterSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pragma_table_info('teachers') WHERE name = ?",
                    Integer.class, columnName);
            if (count == null || count == 0) {
                jdbcTemplate.execute(alterSql);
                logger.info("教师看板迁移：已添加 {} 列", columnName);
            } else {
                logger.debug("教师看板迁移：列 {} 已存在，跳过", columnName);
            }
        } catch (Exception e) {
            logger.warn("教师看板迁移：检查列 {} 时异常（{}），继续启动", columnName, e.getMessage());
        }
    }
}
