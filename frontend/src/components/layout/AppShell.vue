<template>
  <div class="shell">
    <!-- ============ 侧边栏（≥1100px 常驻 / 窄屏抽屉） ============ -->
    <aside class="side" :class="{ open: drawerOpen, collapsed }" aria-label="主导航">
      <div class="side-head">
        <div class="side-brand" @click="go('/home')">
          <div class="logo-mark">🐾</div>
          <div class="brand-txt">
            <b>{{ appStore.settings.systemName || '课堂宠物乐园' }}</b>
            <span>Class Pet Garden</span>
          </div>
        </div>
        <button class="drawer-close" @click="drawerOpen = false" aria-label="关闭菜单">✕</button>
      </div>

      <div class="side-class" @click="go('/settings')" :title="appStore.settings.className">
        <span>🏫</span>
        <span class="cls-name">{{ appStore.settings.className || '未命名班级' }}</span>
        <span class="caret">▾</span>
      </div>

      <nav class="side-nav">
        <div v-for="(group, gi) in navGroups" :key="gi" class="nav-group">
          <div v-if="group.label" class="nav-label">{{ group.label }}</div>
          <router-link
            v-for="item in group.items"
            :key="item.key"
            :to="item.path"
            class="nav-item"
            :class="{ on: isActive(item.path) }"
            :title="collapsed ? item.label : ''"
          >
            <span class="ico">{{ item.icon }}</span>
            <span class="label">{{ item.label }}</span>
            <span v-if="item.badge > 0" class="cnt">{{ item.badge }}</span>
          </router-link>
        </div>

        <div class="nav-group">
          <div class="nav-label">帮助</div>
          <button class="nav-item as-btn" :title="collapsed ? '等级说明' : ''" @click="openGuide">
            <span class="ico">📊</span>
            <span class="label">等级说明</span>
          </button>
        </div>
      </nav>

      <div class="side-foot">
        <button
          class="nav-item as-btn"
          :title="isDark ? '切换到亮色' : '切换到深色'"
          @click="toggleMode"
        >
          <span class="ico">{{ isDark ? '☀️' : '🌙' }}</span>
          <span class="label">{{ isDark ? '亮色模式' : '深色模式' }}</span>
        </button>
        <router-link to="/settings" class="nav-item" :class="{ on: isActive('/settings') }" :title="collapsed ? '系统设置' : ''">
          <span class="ico">⚙️</span><span class="label">系统设置</span>
        </router-link>
        <router-link to="/support" class="nav-item" :class="{ on: isActive('/support') }" :title="collapsed ? '赞赏支持' : ''">
          <span class="ico">💖</span><span class="label">赞赏支持</span>
        </router-link>
        <button class="user-card" :title="`退出登录（${authStore.user?.username || '老师'}）`" @click="handleLogout">
          <span class="avatar">{{ initial }}</span>
          <span class="u-info">
            <b>{{ authStore.user?.username || '老师' }}</b>
            <span>教师账号</span>
          </span>
          <svg class="out" viewBox="0 0 24 24" width="16" height="16" fill="none"
               stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M12 3v9" />
            <path d="M6.6 6.6a8 8 0 1 0 10.8 0" />
          </svg>
        </button>
      </div>

      <div class="guide-host"><LevelGuide ref="guideRef" /></div>
    </aside>

    <div v-if="drawerOpen" class="scrim" @click="drawerOpen = false"></div>

    <!-- ============ 主区域 ============ -->
    <div class="main">
      <header class="topbar">
        <button
          class="hamburger"
          :title="isNarrow ? '打开菜单' : (collapsed ? '展开侧栏' : '收起侧栏')"
          :aria-label="isNarrow ? '打开菜单' : (collapsed ? '展开侧栏' : '收起侧栏')"
          @click="onMenuClick"
        >
          <span></span><span></span><span></span>
        </button>
        <div class="heading">
          <h2>{{ title }}</h2>
          <p v-if="subtitle">{{ subtitle }}</p>
        </div>
        <div class="top-search"><slot name="search" /></div>
        <div class="top-actions"><slot name="actions" /></div>
      </header>

      <main class="content">
        <slot />
      </main>
    </div>

    <!-- ============ 移动端底部导航（<768px） ============ -->
    <nav class="tabbar" aria-label="快捷导航">
      <router-link
        v-for="t in tabItems"
        :key="t.key"
        :to="t.path"
        class="tab"
        :class="{ on: isActive(t.path) }"
      >
        <i>{{ t.icon }}</i>
        <span>{{ t.label }}</span>
      </router-link>
      <button class="tab" :class="{ on: drawerOpen }" @click="drawerOpen = true">
        <i>☰</i>
        <span>更多</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '../../stores/app.js'
import { useAuthStore } from '../../stores/auth.js'
import { CLASSROOM_ENABLED } from '../../config/features.js'
import LevelGuide from '../LevelGuide.vue'
import $confirm from '../../composables/useConfirmModal.js'

defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' }
})

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

const drawerOpen = ref(false)
const guideRef = ref(null)

// 侧栏折叠：桌面端偏好存在本地，刷新后保持
const COLLAPSE_KEY = 'cp_sidebar_collapsed'
const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')

function toggleCollapse() {
  collapsed.value = !collapsed.value
  localStorage.setItem(COLLAPSE_KEY, collapsed.value ? '1' : '0')
}

// 外观模式：直接写回后端 teachers.theme_mode
const isDark = computed(() => appStore.settings?.themeMode === 'dark')

async function toggleMode() {
  const next = isDark.value ? 'light' : 'dark'
  try {
    await appStore.updateSettings({ themeMode: next })
  } catch (e) {
    $confirm.error('外观切换失败，请稍后再试')
  }
}

const navGroups = computed(() => {
  const main = [
    { key: 'home', label: '宠物墙', icon: '🏠', path: '/home' },
    { key: 'students', label: '学生管理', icon: '👥', path: '/students', badge: appStore.students?.length },
    { key: 'leaderboard', label: '光荣榜', icon: '🏆', path: '/leaderboard' },
    { key: 'shop', label: '小卖部', icon: '🏪', path: '/shop', badge: appStore.shopItems?.length }
  ]
  if (CLASSROOM_ENABLED) main.push({ key: 'classroom', label: '课堂', icon: '📚', path: '/classroom' })

  return [
    { items: main },
    {
      label: '数据',
      items: [
        { key: 'history', label: '加减分历史', icon: '📜', path: '/history' },
        { key: 'exchange', label: '兑换明细', icon: '📦', path: '/exchange-history' }
      ]
    },
    {
      label: '玩法',
      items: [{ key: 'pokemon', label: '宝可梦池', icon: '🎮', path: '/pokemon-pool' }]
    }
  ]
})

const tabItems = [
  { key: 'home', label: '宠物墙', icon: '🏠', path: '/home' },
  { key: 'students', label: '学生', icon: '👥', path: '/students' },
  { key: 'leaderboard', label: '光荣榜', icon: '🏆', path: '/leaderboard' },
  { key: 'shop', label: '小卖部', icon: '🏪', path: '/shop' }
]

const initial = computed(() => {
  const n = authStore.user?.username || '师'
  return n.slice(0, 1).toUpperCase()
})

// 窄屏：汉堡 = 打开抽屉；桌面：汉堡 = 收起/展开侧栏
const mq = window.matchMedia('(max-width: 1100px)')
const isNarrow = ref(mq.matches)
function onMqChange(e) { isNarrow.value = e.matches }
mq.addEventListener('change', onMqChange)

function onMenuClick() {
  if (isNarrow.value) drawerOpen.value = true
  else toggleCollapse()
}

function isActive(path) {
  return route.path === path
}

function go(path) {
  drawerOpen.value = false
  if (route.path !== path) router.push(path)
}

function openGuide() {
  drawerOpen.value = false
  guideRef.value?.open()
}

async function handleLogout() {
  const ok = await $confirm.confirm('退出后需要重新输入用户名和密码才能进入，确定要退出登录吗？', {
    title: '退出登录',
    okText: '退出登录',
    cancelText: '再想想',
    icon: '👋',
    danger: true
  })
  if (!ok) return
  drawerOpen.value = false
  authStore.logout()
  router.push('/')
}

// 路由变化自动收起抽屉（含浏览器前进后退）
watch(() => route.fullPath, () => { drawerOpen.value = false })

// 抽屉打开时锁定页面滚动，关闭时恢复
watch(drawerOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

function onKeydown(e) {
  if (e.key === 'Escape') drawerOpen.value = false
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  mq.removeEventListener('change', onMqChange)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.shell {
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--bg);
}

/* ---------- 侧边栏 ---------- */
.side {
  width: var(--sidebar-w);
  flex: 0 0 var(--sidebar-w);
  background: var(--surface);
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  padding: 18px 14px 14px;
  position: sticky;
  top: 0;
  height: 100vh;
  height: 100dvh;
  overflow-y: auto;
  overscroll-behavior: contain;
  z-index: 40;
}
.side-head { display: flex; align-items: center; gap: 8px; }
.side-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 16px;
  cursor: pointer;
  flex: 1;
  min-width: 0;
}
.logo-mark {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 19px;
  background: linear-gradient(140deg, #fff, var(--brand-soft));
  box-shadow: 0 6px 16px var(--brand-soft-2), inset 0 0 0 1px rgba(255, 255, 255, .9);
}
.brand-txt { min-width: 0; }
.brand-txt b {
  display: block;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.brand-txt span { font-size: 11px; color: var(--ink-3); }

.drawer-close {
  display: none;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  margin-bottom: 14px;
  border: 0;
  border-radius: 12px;
  background: var(--line-soft);
  color: var(--ink-2);
  font-size: 15px;
  cursor: pointer;
}

.side-class {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 4px 14px;
  padding: 9px 11px;
  border-radius: 11px;
  background: var(--brand-soft);
  border: 1px solid var(--brand-soft-2);
  font-size: 12.5px;
  color: var(--brand-deep);
  font-weight: 600;
  cursor: pointer;
  transition: .16s;
}
.side-class:hover { filter: brightness(.98); }
.cls-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.caret { font-size: 9px; opacity: .55; }

.side-nav { flex: 1; }
.nav-label {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--ink-4);
  letter-spacing: .08em;
  padding: 12px 12px 6px;
}
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 11px;
  border: 0;
  background: transparent;
  border-radius: 11px;
  font-size: 13.5px;
  font-family: inherit;
  text-align: left;
  color: var(--ink-2);
  text-decoration: none;
  cursor: pointer;
  transition: .16s;
  margin-bottom: 2px;
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
}
.nav-item:hover { background: var(--line-soft); color: var(--ink); }
.nav-item .ico {
  width: 26px;
  height: 26px;
  flex: 0 0 26px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  background: var(--line-soft);
  transition: .16s;
}
.nav-item .label { flex: 1; }
.nav-item .cnt {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--ink-3);
  background: var(--line-soft);
  padding: 1px 6px;
  border-radius: 6px;
}
.nav-item.on {
  background: linear-gradient(90deg, var(--brand-soft), rgba(255, 255, 255, 0));
  color: var(--brand-deep);
  font-weight: 600;
}
.nav-item.on .ico {
  background: linear-gradient(140deg, var(--brand), var(--brand-deep));
  color: #fff;
  box-shadow: 0 4px 10px var(--brand-soft-2);
}
.nav-item.on::before {
  content: "";
  position: absolute;
  left: -7px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  border-radius: 0 3px 3px 0;
  background: var(--brand);
}
.nav-item.on .cnt { background: var(--brand-soft); color: var(--brand-deep); }

.side-foot { margin-top: auto; padding-top: 12px; border-top: 1px solid var(--line); }
.user-card {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 8px;
  margin-top: 6px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  cursor: pointer;
  transition: .16s;
  text-align: left;
  font-family: inherit;
  touch-action: manipulation;
}
.user-card:hover { background: var(--line-soft); }
.avatar {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--brand-deep);
  background: linear-gradient(140deg, var(--brand-soft-2), var(--brand-soft));
}
.u-info { min-width: 0; flex: 1; }
.u-info b {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.u-info span { font-size: 10.5px; color: var(--ink-3); }
.out { font-size: 13px; color: var(--ink-4); }

.guide-host { display: none; }

/* ---------- 主区域 ---------- */
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }

.topbar {
  height: var(--topbar-h);
  flex: 0 0 var(--topbar-h);
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 26px;
  background: color-mix(in srgb, var(--surface) 82%, transparent);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 30;
}
.hamburger {
  display: flex;
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: var(--surface);
  cursor: pointer;
  padding: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  touch-action: manipulation;
}
.hamburger span { display: block; width: 15px; height: 1.6px; border-radius: 2px; background: var(--ink-2); }

.heading { min-width: 0; }
.heading h2 {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.heading p { font-size: 11.5px; color: var(--ink-3); margin-top: 2px; }
.top-search { margin-left: auto; min-width: 0; }
.top-actions { display: flex; align-items: center; gap: 8px; }

.content {
  flex: 1;
  padding: 22px 26px 26px;
  min-width: 0;
}

.scrim {
  position: fixed;
  inset: 0;
  background: rgba(20, 22, 30, .38);
  backdrop-filter: blur(2px);
  z-index: 39;
}

.tabbar { display: none; }

/* ---------- 折叠态（仅桌面） ---------- */
@media (min-width: 1101px) {
  .side { transition: width .22s ease, flex-basis .22s ease, padding .22s ease; }
  .side.collapsed { width: 76px; flex: 0 0 76px; padding: 18px 10px 14px; }
  .side.collapsed .brand-txt,
  .side.collapsed .cls-name,
  .side.collapsed .caret,
  .side.collapsed .nav-item .label,
  .side.collapsed .nav-item .cnt,
  .side.collapsed .nav-label,
  .side.collapsed .u-info,
  .side.collapsed .out { display: none; }

  .side.collapsed .side-brand { padding: 4px 0 14px; justify-content: center; }
  .side.collapsed .side-class { justify-content: center; padding: 9px 0; margin: 0 0 12px; }
  .side.collapsed .nav-item { justify-content: center; padding: 10px 0; gap: 0; }
  .side.collapsed .nav-item .ico { width: 30px; height: 30px; flex-basis: 30px; font-size: 14px; }
  .side.collapsed .nav-item.on::before { left: -10px; }
  .side.collapsed .user-card { justify-content: center; padding: 8px 0; }
  .side.collapsed .nav-group + .nav-group {
    border-top: 1px solid var(--line);
    margin-top: 8px;
    padding-top: 8px;
  }
}

/* ---------- 响应式 ---------- */
@media (max-width: 1100px) {
  .side {
    position: fixed;
    left: 0;
    top: 0;
    height: 100dvh;
    z-index: 50;
    transform: translateX(-102%);
    transition: transform .26s cubic-bezier(.32, .72, 0, 1);
    box-shadow: 0 20px 50px rgba(20, 22, 30, .18);
  }
  .side.open { transform: translateX(0); }
  .drawer-close { display: flex; align-items: center; justify-content: center; }
}

@media (max-width: 768px) {
  .topbar { padding: 0 12px; gap: 9px; height: 58px; flex: 0 0 58px; }
  .heading { flex: 0 1 auto; }
  .heading h2 { font-size: 15px; }
  .heading p { display: none; }
  .top-search { flex: 1; margin-left: 2px; }

  .content {
    padding: 14px 14px calc(76px + env(safe-area-inset-bottom));
  }

  /* 侧栏在移动端变成抽屉，条目加大到易点区域 */
  .side {
    width: min(82vw, 300px);
    flex-basis: auto;
    padding: 14px 12px calc(22px + env(safe-area-inset-bottom));
  }
  .nav-item { padding: 12px 11px; font-size: 14.5px; }
  .nav-item .ico { width: 30px; height: 30px; flex-basis: 30px; font-size: 15px; }
  .side-class { padding: 11px; font-size: 13px; }

  .top-actions .btn-primary { padding: 9px 12px; font-size: 13px; }

  .tabbar {
    display: flex;
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    height: calc(58px + env(safe-area-inset-bottom));
    padding-bottom: env(safe-area-inset-bottom);
    background: color-mix(in srgb, var(--surface) 94%, transparent);
    backdrop-filter: blur(14px);
    border-top: 1px solid var(--line);
    z-index: 45;
  }
  .tab {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 2px;
    position: relative;
    font-size: 10px;
    color: var(--ink-4);
    text-decoration: none;
    background: none;
    border: 0;
    cursor: pointer;
    font-family: inherit;
    touch-action: manipulation;
    -webkit-tap-highlight-color: transparent;
  }
  .tab i {
    font-style: normal;
    font-size: 18px;
    line-height: 1;
    padding: 4px 12px;
    border-radius: 10px;
    transition: background .18s ease, transform .18s ease;
  }
  .tab.on { color: var(--brand-deep); font-weight: 700; }
  .tab.on i { background: var(--brand-soft); transform: translateY(-1px); }
}
</style>
