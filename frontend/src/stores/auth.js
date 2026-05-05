import { defineStore } from 'pinia'
import api from '../api/index.js'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    user: JSON.parse(localStorage.getItem('user') || 'null'),
  }),
  getters: {
    isLoggedIn: (s) => s.token !== null && s.token !== '',
    isActivated: (s) => s.user?.activated ?? true,
    teacherId: (s) => s.user?.teacherId ?? null,
    role: (s) => s.user?.role ?? 'teacher',
    isStudent: (s) => s.user?.role === 'student',
    studentId: (s) => s.user?.studentId ?? null,
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
        activated: true
      })
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
    async validateToken() {
      try {
        const { data } = await api.get('/auth/validate')
        if (!data.valid) {
          this.clearAuth()
          return false
        }
        return true
      } catch (e) {
        this.clearAuth()
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
