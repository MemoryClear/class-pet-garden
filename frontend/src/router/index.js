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
    meta: { requiresAuth: true, notActivated: true }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/HomeView.vue'),
    meta: { requiresAuth: true, activated: true }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('../views/HistoryView.vue'),
    meta: { requiresAuth: true, activated: true }
  },
  {
    path: '/exchange-history',
    name: 'ExchangeHistory',
    component: () => import('../views/ExchangeHistoryView.vue'),
    meta: { requiresAuth: true, activated: true }
  },
  {
    path: '/leaderboard',
    name: 'Leaderboard',
    component: () => import('../views/LeaderboardView.vue'),
    meta: { requiresAuth: true, activated: true }
  },
  {
    path: '/shop',
    name: 'Shop',
    component: () => import('../views/ShopView.vue'),
    meta: { requiresAuth: true, activated: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/SettingsView.vue'),
    meta: { requiresAuth: true, activated: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from) => {
  const auth = useAuthStore()
  // 验证 token 有效性
  if (auth.token) {
    const checkResult = await auth.checkAuth()
    if (checkResult === false) {
      // token 无效，清除并跳转登录页
      return { name: 'Login' }
    }
    if (checkResult?.needActivate) {
      return { name: 'Activate' }
    }
  }
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'Login' }
  }
  if (to.meta.guest && auth.isLoggedIn) {
    if (!auth.isActivated) return { name: 'Activate' }
    return { name: 'Home' }
  }
  if (to.meta.notActivated && auth.isActivated) {
    return { name: 'Home' }
  }
  if (to.meta.activated && !auth.isActivated) {
    return { name: 'Activate' }
  }
})

export default router