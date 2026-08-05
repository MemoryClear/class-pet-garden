package com.classpet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 功能开关配置。后端启动时从 application.properties 读取，对应环境变量
 * {@code CLASSROOM_ENABLED}（application.properties 里映射为 {@code classroom.enabled}）。
 *
 * <p>前端通过 {@code /api/features} 端点拿这个值，避免 build-time 烧死。</p>
 */
@Component
@ConfigurationProperties(prefix = "classroom")
public class FeaturesConfig {

    /** 课堂功能开关。默认 false。 */
    private boolean enabled = false;

    public boolean isClassroomEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}