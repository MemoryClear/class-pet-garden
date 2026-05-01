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

// 响应拦截器：401 → 跳转登录
api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      localStorage.clear()
      router.push('/')
    }
    return Promise.reject(err)
  }
)

export default api