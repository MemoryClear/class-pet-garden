package com.classpet.controller;

import com.classpet.config.FeaturesConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 功能开关端点：前端运行时拉取，避免 build-time 烧死。
 *
 * <p>两个职责：
 * <ol>
 *   <li>{@code GET /api/features}：返回 JSON 开关（前端用于按钮/路由判断）。</li>
 *   <li>{@code GET /} 或 {@code /index.html}：在 index.html 的 {@code <head>} 里注入
 *       {@code window.__APP_CONFIG__}，保证 Vite 打包后的 SPA 启动时就能读到配置。</li>
 * </ol>
 *
 * <p>注意：第二个职责与 SpaController 冲突，所以这里只覆盖 / 和 /index.html 两条路径，
 * 其他 SPA 路由仍然走 SpaController forward。</p>
 */
@RestController
public class FeaturesController {

    private final FeaturesConfig featuresConfig;

    public FeaturesController(FeaturesConfig featuresConfig) {
        this.featuresConfig = featuresConfig;
    }

    /**
     * 返回功能开关 JSON。
     * 公开（无认证）—— 开关状态不是敏感信息。
     */
    @GetMapping("/api/features")
    public Map<String, Object> features() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("classroomEnabled", featuresConfig.isClassroomEnabled());
        return m;
    }

    /**
     * 覆写 / 路径：注入配置后返回 index.html。
     * SpaController 仍负责 /home, /activate 等其他路径。
     */
    @GetMapping(value = {"/", "/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> index() throws IOException {
        ClassPathResource res = new ClassPathResource("static/index.html");
        String html = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);

        // 在 </head> 之前注入 config，避免阻塞首屏渲染
        String configScript = "<script>window.__APP_CONFIG__ = "
                + "Object.assign(window.__APP_CONFIG__ || {}, "
                + toJson() + ");</script>";
        if (html.contains("</head>")) {
            html = html.replace("</head>", configScript + "</head>");
        } else {
            // 极端兜底：插到最前面
            html = configScript + html;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private String toJson() {
        // 简单手写避免引 Jackson 进这里；FeaturesConfig 字段都是 boolean，序列化安全
        return "{classroomEnabled:" + featuresConfig.isClassroomEnabled() + "}";
    }
}