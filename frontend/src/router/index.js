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

router.beforeEach(async (to, from) => {
  const auth = useAuthStore()
  if (auth.token) {
    const checkResult = await auth.checkAuth()
    if (checkResult === false) {
      // 验证失败：token 已被清除。避免重复跳转调 /auth/validate 造成死循环，直接放行让后续 guard 重定向。
      if (to.name === 'Login') return true
      return { name: 'Login' }
    }
    // 学生角色路由守卫
    if (auth.isStudent) {
      // 强制改密拦截：除改密页外，强制跳到改密页
      if (auth.mustChangePassword && to.name !== 'ChangePassword') {
        return { name: 'ChangePassword' }
      }
      // 学生访问非学生页面 → 学生首页
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
  // 改密页强制：改完后才能离开
  if (to.meta.forceChangePassword && auth.isLoggedIn && !auth.mustChangePassword) {
    return auth.isStudent ? { name: 'StudentHome' } : { name: 'Home' }
  }
  // 课堂功能禁用时：访问 /classroom 一律重定向到首页
  if (to.meta.classroomFeature && !CLASSROOM_ENABLED) {
    return auth.isStudent ? { name: 'StudentHome' } : { name: 'Home' }
  }
})

export default router
