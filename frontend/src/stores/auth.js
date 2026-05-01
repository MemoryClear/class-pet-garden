import { defineStore } from 'pinia'
import api from '../api/index.js'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isActivated: (s) => s.user?.activated ?? false,
    teacherId: (s) => s.user?.teacherId ?? null,
  },
  actions: {
    setAuth(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    clearAuth() {
      this.token = ''
      this.user = null
      localStorage.clear()
    },
    async login(username, password) {
      const { data } = await api.post('/auth/login', { username, password })
      this.setAuth(data.token, data)
      return data
    },
    async register(username, password, confirmPassword) {
      const { data } = await api.post('/auth/register', { username, password, confirmPassword })
      this.setAuth(data.token, data)
      return data
    },
    async activate(code) {
      const { data } = await api.post('/auth/activate', { code })
      this.setAuth(data.token, data)
      return data
    },
    logout() {
      this.clearAuth()
    }
  }
})