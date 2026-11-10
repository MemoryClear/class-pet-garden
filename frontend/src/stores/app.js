import { defineStore } from 'pinia'
import api from '../api/index.js'

export const useAppStore = defineStore('app', {
  state: () => ({
    students: [],
    scoreItems: [],
    petLibrary: [],
    pokemonLibrary: [],
    pokemonLibraryLoadedAt: 0,
    history: [],
    historyHasMore: false,
    historyNextCursor: null,
    historyLoading: false,
    leaderboard: [],
    shopItems: [],
    exchangeRecords: [],
    exchangeRecordsHasMore: false,
    exchangeRecordsNextCursor: null,
    exchangeRecordsLoading: false,
    settings: { systemName: '班级宠物园', className: '默认班级', theme: 'pink' },
    // ===== 多班级 =====
    classes: [],
    currentClassId: localStorage.getItem('cp_current_class') || null,
    loading: false,
  }),
  getters: {
    /** 当前班级对象 */
    currentClass(state) {
      return state.classes.find(c => c.id === state.currentClassId) || null
    },
    /** 侧栏/标题用的班级名 */
    className(state) {
      const c = state.classes.find(x => x.id === state.currentClassId)
      return c ? c.name : (state.settings.className || '未命名班级')
    }
  },
  actions: {
    // ===== 班级 =====
    async fetchClasses() {
      const { data } = await api.get('/classes')
      this.classes = Array.isArray(data) ? data : []
      // 当前班级失效（被删 / 首次登录）时落到第一个
      if (!this.currentClassId || !this.classes.some(c => c.id === this.currentClassId)) {
        this.setCurrentClass(this.classes[0]?.id || null)
      }
      return this.classes
    },
    setCurrentClass(id) {
      this.currentClassId = id || null
      if (id) localStorage.setItem('cp_current_class', id)
      else localStorage.removeItem('cp_current_class')
    },
    /** 切换班级：写本地偏好 + 重新拉取班级维度数据 */
    async switchClass(id) {
      if (!id || id === this.currentClassId) return
      this.setCurrentClass(id)
      await Promise.all([
        this.fetchStudents(),
        this.fetchLeaderboard(),
        this.fetchHistory(),
        this.fetchExchangeRecords({ reset: true })
      ]).catch(() => {})
    },

    // Students
    async fetchStudents() {
      const { data } = await api.get('/students')
      this.students = data
    },
    async createStudent(name) {
      const { data } = await api.post('/students', { name, classId: this.currentClassId })
      await this.fetchStudents()
      // 学生数变了，班级列表里的 studentCount 也要跟着变（班级管理页直接用这份数据）
      this.fetchClasses().catch(() => {})
      return data
    },
    async batchCreateStudents(names) {
      const { data } = await api.post('/students/batch', { names, classId: this.currentClassId })
      this.students = data
      this.fetchClasses().catch(() => {})
    },
    async updateStudent(id, name) {
      const { data } = await api.put(`/students/${id}`, { name })
      await this.fetchStudents()
      return data
    },
    async deleteStudent(id) {
      await api.delete(`/students/${id}`)
      await this.fetchStudents()
      this.fetchClasses().catch(() => {})
    },
    async adoptPet(studentId, petId, petName, petIcon) {
      const name = petName || this.petLibrary.find(p => p.id === petId)?.name || ''
      const icon = petIcon || this.petLibrary.find(p => p.id === petId)?.icon || '❓'
      const { data } = await api.post(`/students/${studentId}/adopt`, {
        petId: typeof petId === 'number' ? petId : parseInt(petId),
        petName: name,
        petIcon: icon
      })
      await this.fetchStudents()
      return data
    },
    async assignPetsRandomly() {
      const { data } = await api.post('/students/assign-pets', null, {
        params: { classId: this.currentClassId }
      })
      this.students = data
    },
    async applyScore(studentId, scoreItemId, multiplier) {
      const body = { scoreItemId }
      if (multiplier && multiplier > 1) body.multiplier = multiplier
      const { data } = await api.post(`/students/${studentId}/score`, body)
      await this.fetchStudents()
      return data
    },
    // Leaderboard
    async fetchLeaderboard() {
      const { data } = await api.get('/students/leaderboard')
      this.leaderboard = data
    },
    async fetchTotalScoreLeaderboard() {
      const { data } = await api.get('/students/leaderboard/total')
      return data
    },
    // Shop Items
    async fetchShopItems() {
      const { data } = await api.get('/shop/items')
      this.shopItems = data
    },
    async addShopItem(item) {
      const { data } = await api.post('/shop/items', item)
      await this.fetchShopItems()
      return data
    },
    async updateShopItem(id, item) {
      const { data } = await api.put(`/shop/items/${id}`, item)
      await this.fetchShopItems()
      return data
    },
    async deleteShopItem(id) {
      await api.delete(`/shop/items/${id}`)
      await this.fetchShopItems()
    },
    // Equip / Unequip decorations
    async equipItem(studentId, itemId) {
      const { data } = await api.post(`/students/${studentId}/equip`, { itemId })
      await this.fetchStudents()
      return data
    },
    async unequipItem(studentId, itemId) {
      const { data } = await api.delete(`/students/${studentId}/equip/${itemId}`)
      await this.fetchStudents()
      return data
    },
    // Exchange
    async exchangeItem(studentId, itemId) {
      const { data } = await api.post('/shop/exchange', { studentId, itemId })
      await this.fetchStudents()
      await this.fetchExchangeRecords({ reset: true })
      return data
    },
    async fetchExchangeRecords(opts = {}) {
      const { reset = true, limit = 20 } = opts
      this.exchangeRecordsLoading = true
      try {
        const params = { limit }
        if (!reset && this.exchangeRecordsNextCursor) {
          params.cursorTime = this.exchangeRecordsNextCursor.createdAt
          params.cursorId = this.exchangeRecordsNextCursor.id
        }
        const { data } = await api.get('/shop/records', { params })
        if (reset) {
          this.exchangeRecords = data.items || []
        } else {
          this.exchangeRecords = [...this.exchangeRecords, ...(data.items || [])]
        }
        this.exchangeRecordsHasMore = !!data.hasMore
        this.exchangeRecordsNextCursor = data.nextCursor || null
      } finally {
        this.exchangeRecordsLoading = false
      }
    },
    // Gift item to another student
    async giftItem(fromStudentId, toStudentId, recordId) {
      const { data } = await api.post('/shop/gift', { fromStudentId, toStudentId, recordId })
      await this.fetchStudents()
      await this.fetchExchangeRecords({ reset: true })
      return data
    },
    // Score Items
    async fetchScoreItems() {
      const { data } = await api.get('/score-items')
      this.scoreItems = data
    },
    async createScoreItem(icon, name, point) {
      await api.post('/score-items', { icon, name, point })
      await this.fetchScoreItems()
    },
    async updateScoreItem(id, icon, name, point) {
      await api.put(`/score-items/${id}`, { icon, name, point })
      await this.fetchScoreItems()
    },
    async deleteScoreItem(id) {
      await api.delete(`/score-items/${id}`)
      await this.fetchScoreItems()
    },
    // Pets
    async fetchPetLibrary() {
      const { data } = await api.get('/pets')
      this.petLibrary = data
    },
    // History
    async fetchHistory(studentId, from, to, opts = {}) {
      const { reset = true, limit = 20 } = opts
      this.historyLoading = true
      try {
        const params = { limit }
        if (studentId) params.studentId = studentId
        if (from) params.from = from
        if (to) params.to = to
        if (!reset && this.historyNextCursor) {
          params.cursorTime = this.historyNextCursor.createdAt
          params.cursorId = this.historyNextCursor.id
        }
        const { data } = await api.get('/history', { params })
        if (reset) {
          this.history = data.items || []
        } else {
          this.history = [...this.history, ...(data.items || [])]
        }
        this.historyHasMore = !!data.hasMore
        this.historyNextCursor = data.nextCursor || null
      } finally {
        this.historyLoading = false
      }
    },
    async revokeScore(historyId) {
      const { data } = await api.post(`/history/${historyId}/revoke`)
      await this.fetchHistory()
      await this.fetchStudents()
      return data
    },
    // Settings
    async fetchSettings() {
      const { data } = await api.get('/settings')
      this.settings = { ...this.settings, ...data }
    },
    async updateSettings(payload) {
      const { data } = await api.put('/settings', payload)
      this.settings = { ...this.settings, ...data }
    },
  }
})