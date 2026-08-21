import { defineStore } from 'pinia'
import api from '../api/index.js'

// validateToken 缓存有效期：5 分钟内不重复请求 /api/auth/validate
const VALIDATE_CACHE_MS = 5 * 60 * 1000
const VALIDATE_CACHE_LS_KEY = '_validateCache'

function loadValidateCache() {
  try {
    const raw = localStorage.getItem(VALIDATE_CACHE_LS_KEY)
    if (!raw) return { token: null, at: 0 }
    const obj = JSON.parse(raw)
    return { token: obj.token || null, at: Number(obj.at) || 0 }
  } catch (_) {
    return { token: null, at: 0 }
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => {
    const c = loadValidateCache()
    return {
      token: localStorage.getItem('token') || null,
      user: JSON.parse(localStorage.getItem('user') || 'null'),
      _lastValidatedAt: c.at,
      _lastValidatedToken: c.token,
    }
  },
  getters: {
    isLoggedIn: (s) => s.token !== null && s.token !== '',
    isActivated: (s) => s.user?.activated ?? true,
    teacherId: (s) => s.user?.teacherId ?? null,
    role: (s) => s.user?.role ?? 'teacher',
    isStudent: (s) => s.user?.role === 'student',
    studentId: (s) => s.user?.studentId ?? null,
    mustChangePassword: (s) => Boolean(s.user?.mustChangePassword),
  },
  actions: {
    setAuth(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    clearAuth() {
      this.token = null
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    setMustChangePassword(v) {
      if (this.user) {
        this.user.mustChangePassword = v
        localStorage.setItem('user', JSON.stringify(this.user))
      }
    },
    async login(username, password) {
      const { data } = await api.post('/auth/login', { username, password })
      this.setAuth(data.token, { ...data, role: 'teacher' })
      return data
    },
    async studentLogin(studentNo, password) {
      const { data } = await api.post('/auth/student-login', { studentNo, password })
      this.setAuth(data.token, {
        role: 'student',
        studentId: data.studentId,
        studentNo: data.studentNo,
        studentName: data.studentName,
        teacherId: data.teacherId,
        activated: true,
        mustChangePassword: Boolean(data.mustChangePassword)
      })
      return data
    },
    async studentChangePassword(newPassword) {
      const { data } = await api.post('/auth/student-change-password', { newPassword })
      this.setMustChangePassword(false)
      return data
    },
    async register(username, password, confirmPassword) {
      const { data } = await api.post('/auth/register', { username, password, confirmPassword })
      this.setAuth(data.token, { ...data, role: 'teacher' })
      return data
    },
    async activate(code) {
      const { data } = await api.post('/auth/activate', { code })
      this.setAuth(data.token, { ...data, role: 'teacher' })
      return data
    },
    async validateToken(force = false) {
      // 缓存命中：上一次验证后未超过有效期，则跳过网络请求。
      // 注意：不要求 token 字符串严格相等——后端会在 validate 响应头返回 X-New-Token
      // 滑动续期，api/index.js 的 response interceptor 会同步更新 localStorage.token 和 Pinia.token。
      // 只要上次验证后 5 分钟内，新 token 必然已由旧 token 合法签发。
      const now = Date.now()
      if (
        !force &&
        this._lastValidatedAt > 0 &&
        now - this._lastValidatedAt < VALIDATE_CACHE_MS
      ) {
        // 保持 _lastValidatedToken 与当前 token 同步（被 X-New-Token 漂过也兼容）
        this._lastValidatedToken = this.token
        return true
      }
      try {
        const { data } = await api.get('/auth/validate')
        if (!data.valid) {
          this.clearAuth()
          this._lastValidatedAt = 0
          this._lastValidatedToken = null
          localStorage.removeItem(VALIDATE_CACHE_LS_KEY)
          return false
        }
        this._lastValidatedAt = now
        this._lastValidatedToken = this.token
        localStorage.setItem(
          VALIDATE_CACHE_LS_KEY,
          JSON.stringify({ token: this.token, at: now })
        )
        return true
      } catch (e) {
        // 401/403 已由 api interceptor 局部处理；这里只重置状态。
        this.clearAuth()
        this._lastValidatedAt = 0
        this._lastValidatedToken = null
        localStorage.removeItem(VALIDATE_CACHE_LS_KEY)
        return false
      }
    },
    logout() {
      this.clearAuth()
    },
    async checkAuth() {
      if (!this.token) return false
      const valid = await this.validateToken()
      if (!valid) return false
      if (!this.isStudent && !this.user?.activated) {
        return { needActivate: true }
      }
      return true
    }
  }
})
