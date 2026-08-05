package com.classpet.controller;

import com.classpet.migration.StudentPasswordMigration;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.classpet.security.JwtAuthenticationFilter.AuthenticatedUser;

/**
 * 运维/调试用端点：查询学生密码迁移状态。
 *
 * <p>需要教师认证（学生 token 没权限看全表统计）。</p>
 */
@RestController
@RequestMapping("/api/admin")
public class MigrationController {

    private final StudentPasswordMigration migration;

    public MigrationController(StudentPasswordMigration migration) {
        this.migration = migration;
    }

    @GetMapping("/student-migration-status")
    public StudentPasswordMigration.MigrationStatus status(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        // 任何已认证用户（教师）都可以查询
        return migration.getStatus();
    }
}