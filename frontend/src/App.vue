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

const confirmModal = ref(null)
const appStore = useAppStore()
const authStore = useAuthStore()

// 主题：把设置里的 theme 写到 <html data-theme>，驱动全局 CSS 变量
const theme = computed(() => appStore.settings?.theme || 'pink')
watch(theme, (t) => document.documentElement.setAttribute('data-theme', t), { immediate: true })

// 外观模式：light / dark，默认亮色（存在 teachers.theme_mode）
const mode = computed(() => (appStore.settings?.themeMode === 'dark' ? 'dark' : 'light'))
watch(mode, (m) => {
  document.documentElement.setAttribute('data-mode', m)
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
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body {
  background: #FBF8F7;
  min-height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #22242F;
  -webkit-font-smoothing: antialiased;
}
</style>
