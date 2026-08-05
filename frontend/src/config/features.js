/**
 * 功能开关：运行时读取，支持 build-time 和 run-time 两种来源。
 *
 * 优先级（高 → 低）：
 *   1. window.__APP_CONFIG__   — Docker 容器启动时由后端 FeaturesController 注入
 *                                 （对应环境变量 CLASSROOM_ENABLED）
 *   2. import.meta.env.VITE_* — Vite 构建时烧死（dev / 本地 build 用）
 *   3. 代码内 hardcoded 默认    — 上面都没拿到时的兜底
 *
 * 后端配置怎么注入：见 backend/src/main/java/com/classpet/controller/FeaturesController.java
 * 它在返回 index.html 时把 window.__APP_CONFIG__ = {classroomEnabled: ...} 塞到 </head> 前面。
 *
 * 部署 docker 镜像时设置：
 *   docker run -e CLASSROOM_ENABLED=true class-pet-garden
 *   docker run -e CLASSROOM_ENABLED=false class-pet-garden
 */

const DEFAULT_CLASSROOM_ENABLED = false

function readClassroomEnabled () {
    // 1. 运行时注入（Docker / 生产部署）
    if (typeof window !== 'undefined' && window.__APP_CONFIG__
        && typeof window.__APP_CONFIG__.classroomEnabled === 'boolean') {
        return window.__APP_CONFIG__.classroomEnabled
    }
    // 2. 构建时环境变量（Vite）
    const env = import.meta.env
    if (env && env.VITE_CLASSROOM_ENABLED != null) {
        return String(env.VITE_CLASSROOM_ENABLED).toLowerCase() === 'true'
    }
    // 3. 默认值
    return DEFAULT_CLASSROOM_ENABLED
}

/** 课堂功能：进入按钮 + /classroom 路由 */
export const CLASSROOM_ENABLED = readClassroomEnabled()

if (typeof window !== 'undefined' && import.meta.env && import.meta.env.DEV) {
    console.info('[features] CLASSROOM_ENABLED =', CLASSROOM_ENABLED,
        '(source:', window.__APP_CONFIG__ ? 'window.__APP_CONFIG__' :
                    (import.meta.env.VITE_CLASSROOM_ENABLED != null ? 'VITE_* env' : 'default'),
        ')')
}