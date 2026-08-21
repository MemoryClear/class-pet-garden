import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { CLASSROOM_ENABLED } from '../config/features.js'

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
    path: '/support',
    name: 'Support',
    component: () => import('../views/SupportView.vue'),
    meta: { requiresAuth: true, activated: true, teacherOnly: true }
  },
  {
    path: '/classroom',
    name: 'Classroom',
    component: () => import('../views/ClassroomView.vue'),
    meta: { requiresAuth: true, classroomFeature: true }
  },
  {
    path: '/pokemon-pool',
    name: 'PokemonPool',
    component: () => import('../views/PokemonPoolView.vue'),
    meta: { requiresAuth: true, activated: true, teacherOnly: true }
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('../views/ChangePasswordView.vue'),
    meta: { requiresAuth: true, forceChangePassword: true }
  },
  {
    path: '/board',
    name: 'Board',
    component: () => import('../views/BoardView.vue'),
    meta: { guest: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 死循环防护：在同一次导航链中，对同一 token 只 validate 一次。
// 防止 401 触发 router.push 后 beforeEach 再次调 /api/auth/validate 造成死循环。
let _validatedTokenInNav = null

router.beforeEach(async (to, from) => {
  const auth = useAuthStore()

  // 以 localStorage 为唯一权威同步 Pinia 状态（避免 Pinia 与 localStorage 不一致）
  const lsToken = localStorage.getItem('token')
  if (auth.token && !lsToken) {
    auth.clearAuth()
  } else if (!auth.token && lsToken) {
    auth.token = lsToken
    try {
      auth.user = JSON.parse(localStorage.getItem('user') || 'null')
    } catch (_) {
      auth.user = null
    }
  }

  if (auth.token) {
    // 死循环防护：同一次导航链中同一 token 只 validate 一次（避免并发 await race）
    // checkAuth 内部 validateToken 已有 5 分钟缓存，所以即使跨导航也只发一次网络请求
    if (_validatedTokenInNav !== auth.token) {
      const checkResult = await auth.checkAuth()
      _validatedTokenInNav = auth.token
      if (checkResult === false) {
        if (to.name === 'Login') return true
        return { name: 'Login' }
      }
      // 验证通过后再判断教师激活状态
      if (!auth.isStudent && !auth.isActivated && to.name !== 'Activate') {
        return { name: 'Activate' }
      }
    }

    // 学生角色路由守卫
    if (auth.isStudent) {
      if (auth.mustChangePassword && to.name !== 'ChangePassword') {
        return { name: 'ChangePassword' }
      }
      if (to.name !== 'ChangePassword') {
        if (to.name === 'StudentHome') return true
        if (to.meta.studentOnly) return true
        if (to.name !== 'Login' && to.name !== 'StudentHome') {
          return { name: 'StudentHome' }
        }
      } else {
        return true
      }
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
  if (to.meta.forceChangePassword && auth.isLoggedIn && !auth.mustChangePassword) {
    return auth.isStudent ? { name: 'StudentHome' } : { name: 'Home' }
  }
  if (to.meta.classroomFeature && !CLASSROOM_ENABLED) {
    return auth.isStudent ? { name: 'StudentHome' } : { name: 'Home' }
  }
})

// validateToken 已有 5 分钟缓存，重复调 checkAuth 也会命中缓存不发起网络请求
// 因此不再需要在 afterEach 重置防护标记（保留纯粹防并发 race）

export default router