import { defineStore } from 'pinia'
import api from '../api/index.js'

export const useAppStore = defineStore('app', {
  state: () => ({
    students: [],
    scoreItems: [],
    petLibrary: [],
    history: [],
    leaderboard: [],
    shopItems: [],
    exchangeRecords: [],
    settings: { systemName: '班级宠物园', className: '默认班级', theme: 'pink' },
    loading: false,
  }),
  actions: {
    // Students
    async fetchStudents() {
      const { data } = await api.get('/students')
      this.students = data
    },
    async createStudent(name) {
      const { data } = await api.post('/students', { name })
      await this.fetchStudents()
      return data
    },
    async batchCreateStudents(names) {
      const { data } = await api.post('/students/batch', { names })
      this.students = data
    },
    async updateStudent(id, name) {
      const { data } = await api.put(`/students/${id}`, { name })
      await this.fetchStudents()
      return data
    },
    async deleteStudent(id) {
      await api.delete(`/students/${id}`)
      await this.fetchStudents()
    },
    async adoptPet(studentId, petId) {
      const pet = this.petLibrary.find(p => p.id === petId)
      const { data } = await api.post(`/students/${studentId}/adopt`, {
        petId: pet.id,
        petName: pet.name,
        petIcon: pet.icon
      })
      await this.fetchStudents()
      return data
    },
    async assignPetsRandomly() {
      const { data } = await api.post('/students/assign-pets')
      this.students = data
    },
    async applyScore(studentId, scoreItemId) {
      const { data } = await api.post(`/students/${studentId}/score`, { scoreItemId })
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
      await this.fetchExchangeRecords()
      return data
    },
    async fetchExchangeRecords() {
      const { data } = await api.get('/shop/records')
      this.exchangeRecords = data
    },
    // Gift item to another student
    async giftItem(fromStudentId, toStudentId, recordId) {
      const { data } = await api.post('/shop/gift', { fromStudentId, toStudentId, recordId })
      await this.fetchStudents()
      await this.fetchExchangeRecords()
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
    async fetchHistory(studentId, from, to) {
      const params = {}
      if (studentId) params.studentId = studentId
      if (from) params.from = from
      if (to) params.to = to
      const { data } = await api.get('/history', { params })
      this.history = data
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