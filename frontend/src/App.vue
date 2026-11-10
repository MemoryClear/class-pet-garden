<template>
  <router-view />
  <ConfirmModal ref="confirmModal" />
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import ConfirmModal from './components/ConfirmModal.vue'
import { setConfirmModal } from './composables/useConfirmModal.js'
import { useAppStore } from './stores/app.js'
import { useAuthStore } from './stores/auth.js'
import { studentMode, currentMode } from './composables/useAppearance.js'

const confirmModal = ref(null)
const appStore = useAppStore()
const authStore = useAuthStore()

// 主题：把设置里的 theme 写到 <html data-theme>，驱动全局 CSS 变量
const theme = computed(() => appStore.settings?.theme || 'pink')
watch(theme, (t) => document.documentElement.setAttribute('data-theme', t), { immediate: true })

// 外观模式：light / dark
//   教师：跟账号设置 teachers.theme_mode
//   学生：默认跟所属教师的设置；本机手动切过就用本机选择（见 composables/useAppearance.js）
const mode = computed(() => {
  // 未登录（登录页 / 退出后）固定亮色：登录页是浅色设计，硬套深色会半黑半白
  if (!authStore.isLoggedIn) return 'light'
  if (authStore.role === 'student' && studentMode.value) return studentMode.value
  return appStore.settings?.themeMode === 'dark' ? 'dark' : 'light'
})
watch(mode, (m) => {
  document.documentElement.setAttribute('data-mode', m)
  currentMode.value = m
  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta) meta.setAttribute('content', m === 'dark' ? '#0F1115' : '#FFFFFF')
}, { immediate: true })

onMounted(async () => {
  if (confirmModal.value) {
    setConfirmModal(confirmModal.value)
  }
  // 全局只拉一次设置：主题、系统名、班级名在任何页面都能拿到
  // （之前只有 HomeView 拉，直接访问 /leaderboard 这类页面会退回默认主题）
  if (authStore.isLoggedIn) {
    try {
      await appStore.fetchSettings()
      // 多班级：启动时同步班级列表与当前班级
      await appStore.fetchClasses()
    } catch (e) { /* 静默失败，保留默认值 */ }
  }
})

// 站内登录（不刷新页面）时也要补拉设置，
// 否则学生/教师登录后主题会停在默认亮色，要手动刷新才生效
watch(() => authStore.isLoggedIn, async (loggedIn) => {
  if (!loggedIn) return
  try {
    await appStore.fetchSettings()
    // 站内登录时也把班级列表补上，否则 currentClassId 为空会导致班级维度页面取不到数据
    await appStore.fetchClasses()
  } catch (e) { /* 忽略：保留默认外观 */ }
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body {
  background: var(--bg);
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: var(--ink);
  -webkit-font-smoothing: antialiased;
}
</style>
