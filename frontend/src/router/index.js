import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const routes = [
  {
    path: '/',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/activate',
    name: 'Activate',
    component: () => import('../views/ActivateView.vue'),
    meta: { requiresAuth: true, notActivated: true, teacherOnly: true }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/HomeView.vue'),
    meta: { requiresAuth: true, activated: true, teacherOnly: true }
  },
  {
    path: '/student-home',
    name: 'StudentHome',
    component: () => import('../views/StudentHomeView.vue'),
    meta: { requiresAuth: true, studentOnly: true }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('../views/HistoryView.vue'),
    meta: { requiresAuth: true, activated: true, teacherOnly: true }
  },
  {
    path: '/exchange-history',
    name: 'ExchangeHistory',
    component: () => import('../views/ExchangeHistoryView.vue'),
    meta: { requiresAuth: true, activated: true, teacherOnly: true }
  },
  {
    path: '/leaderboard',
    name: 'Leaderboard',
    component: () => import('../views/LeaderboardView.vue'),
    meta: { requiresAuth: true, activated: true, teacherOnly: true }
  },
  {
    path: '/shop',
    name: 'Shop',
    component: () => import('../views/ShopView.vue'),
    meta: { requiresAuth: true, activated: true, teacherOnly: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/SettingsView.vue'),
    meta: { requiresAuth: true, activated: true, teacherOnly: true }
  },
  {
    path: '/classroom',
    name: 'Classroom',
    component: () => import('../views/ClassroomView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/pokemon-pool',
    name: 'PokemonPool',
    component: () => import('../views/PokemonPoolView.vue'),
    meta: { requiresAuth: true, activated: true, teacherOnly: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from) => {
  const auth = useAuthStore()
  if (auth.token) {
    const checkResult = await auth.checkAuth()
    if (checkResult === false) {
      return { name: 'Login' }
    }
    // 学生角色路由守卫
    if (auth.isStudent) {
      if (to.name === 'StudentHome') return true
      if (to.meta.studentOnly) return true
      // 学生访问其他页面 → 重定向到学生首页
      if (to.name !== 'Login' && to.name !== 'StudentHome') {
        return { name: 'StudentHome' }
      }
    }
    // 教师角色路由守卫
    if (!auth.isStudent && checkResult?.needActivate && to.name !== 'Activate') {
      return { name: 'Activate' }
    }
  }
  // 学生专属页面，非学生不能进
  if (to.meta.studentOnly && !auth.isStudent) {
    return { name: 'Login' }
  }
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'Login' }
  }
  if (to.meta.guest && auth.isLoggedIn) {
    if (auth.isStudent) return { name: 'StudentHome' }
    if (!auth.isActivated) return { name: 'Activate' }
    return { name: 'Home' }
  }
  if (to.meta.notActivated && auth.isActivated) {
    return { name: 'Home' }
  }
  if (to.meta.activated && !auth.isActivated) {
    return { name: 'Activate' }
  }
  if (to.meta.teacherOnly && auth.isStudent) {
    return { name: 'StudentHome' }
  }
})

export default router
