import axios from 'axios'
import router from '../router/index.js'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：附加 JWT Token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器：自动续期 Token + 401 → 跳转登录
api.interceptors.response.use(
  res => {
    // 滑动过期：后端返回 X-New-Token 时自动更新 localStorage 和 Pinia
    const newToken = res.headers['x-new-token']
    if (newToken) {
      localStorage.setItem('token', newToken)
      // 延迟导入避免循环依赖
      import('../stores/auth.js').then(({ useAuthStore }) => {
        const auth = useAuthStore()
        auth.token = newToken
      }).catch(() => {})
    }
    return res
  },
  err => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      localStorage.clear()
      router.push('/')
    }
    return Promise.reject(err)
  }
)

// ============== 认证相关 ==============
export const authApi = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  register: (data) => api.post('/auth/register', data),
  activate: (code) => api.post('/auth/activate', { code }),
  validate: () => api.get('/auth/validate'),
  checkActivate: () => api.get('/auth/check-activate')
}

// ============== 学生管理 ==============
export const studentApi = {
  getAll: () => api.get('/students'),
  create: (data) => api.post('/students', data),
  update: (id, data) => api.put(`/students/${id}`, data),
  delete: (id) => api.delete(`/students/${id}`),
  getHistory: (id) => api.get(`/students/${id}/history`),
  getPokemonCount: (id) => api.get(`/students/${id}/pokemon-count`),
  getStudentPokemon: (id) => api.get(`/students/${id}/pokemon`),
  setRepresentPokemon: (id, pokemonId) => api.put(`/students/${id}/represent-pokemon`, { pokemonId })
}

// ============== 宠物相关 ==============
export const petApi = {
  getAll: () => api.get('/pets'),
  getPokemon: () => api.get('/pets/pokemon'),
  adopt: (studentId, petData) => api.post(`/students/${studentId}/pet`, petData),
  changePet: (studentId, petData) => api.put(`/students/${studentId}/pet`, petData)
}

// ============== 积分相关 ==============
export const scoreApi = {
  getItems: () => api.get('/score-items'),
  addScore: (studentId, itemId) => api.post(`/students/${studentId}/scores`, { itemId }),
  getHistory: (studentId) => api.get(`/students/${studentId}/scores`),
  revokeScore: (historyId) => api.delete(`/history/${historyId}`)
}

// ============== 商店相关 ==============
export const shopApi = {
  getItems: () => api.get('/shop/items'),
  exchange: (studentId, itemId) => api.post(`/shop/exchange`, { studentId, itemId }),
  gift: (fromStudentId, toStudentId, itemId) => api.post('/shop/gift', { fromStudentId, toStudentId, itemId })
}

// ============== 课堂相关 ==============
export const classroomApi = {
  getAll: () => api.get('/classrooms'),
  getById: (id) => api.get(`/classrooms/${id}`),
  create: (data) => api.post('/classrooms', data),
  update: (id, data) => api.put(`/classrooms/${id}`, data),
  delete: (id) => api.delete(`/classrooms/${id}`),
  // 诗词管理
  addPoem: (classroomId, poem) => api.post(`/classrooms/${classroomId}/poems`, poem),
  removePoem: (classroomId, title) => api.delete(`/classrooms/${classroomId}/poems/${encodeURIComponent(title)}`)
}

// ============== 设置相关 ==============
export const settingsApi = {
  get: () => api.get('/settings'),
  update: (data) => api.put('/settings', data)
}

// ============== 学生端专用 API ==============
export const studentApi2 = {
  me: () => api.get('/student/me'),
  adopt: (data) => api.post('/student/adopt', data),
  exchange: (itemId) => api.post('/student/exchange', { itemId }),
  quizScore: (points, reason) => api.post('/student/quiz-score', { points, reason }),
  // 宝可梦相关
  getPokemon: () => api.get('/student/pokemon'),
  adoptPokemon: (data) => api.post('/student/adopt-pokemon', data),
  feedPokemon: (pokemonId, points) => api.post('/student/feed-pokemon', { pokemonId, points }),
  getPokemonCount: () => api.get('/student/pokemon-count'),
  evolvePokemon: (pokemonId, data) => api.post(`/student/pokemon/${pokemonId}/evolve`, data),
  getEvolutionInfo: (pokemonId) => api.get(`/student/pokemon/${pokemonId}/evolution-info`),
  // 进化道具相关
  getEvolutionItems: () => api.get('/student/evolution-items'),
  useEvolutionItem: (pokemonId, itemKey) => api.post(`/student/pokemon/${pokemonId}/use-evolution-item`, { itemKey }),
  // 精灵球相关
  getPokemonBalls: () => api.get('/student/pokemon-balls'),
  usePokemonBall: () => api.post('/student/use-pokemon-ball'),
  setRepresentPokemon: (pokemonId) => api.post('/student/represent-pokemon', { pokemonId })
}


// ============== 宝可梦池管理 ==============
export const pokemonApi = {
  getClassroomPool: (classroomId) => api.get('/pokemon/classroom-pool', { params: { classroomId } }),
  setClassroomPool: (data) => api.post('/pokemon/classroom-pool', data),
  clearClassroomPool: (classroomId) => api.delete('/pokemon/classroom-pool', { params: { classroomId } }),
  distributeBalls: () => api.post('/pokemon/distribute-balls')
}

export default api
