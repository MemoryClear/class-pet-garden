<template>
  <div class="auth-page">
    <div class="logo">🐱</div>
    <h1 class="title">{{ authStore.user?.systemName || '课堂宠物乐园' }}</h1>
    <p class="subtitle">让学习变得更有趣</p>
    <div class="auth-card">
      <div class="auth-tabs">
        <div class="auth-tab" :class="{ active: tab === 'login' }" @click="tab = 'login'">登录</div>
        <div class="auth-tab" :class="{ active: tab === 'register' }" @click="tab = 'register'">注册</div>
      </div>
      <!-- 登录表单 -->
      <div class="auth-form" v-if="tab === 'login'">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <div class="input-wrapper">
            <span class="input-icon">👤</span>
            <input type="text" class="form-input" v-model="loginForm.username" placeholder="请输入用户名" @keyup.enter="handleLogin">
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">密码</label>
          <div class="input-wrapper">
            <span class="input-icon">🔑</span>
            <input type="password" class="form-input" v-model="loginForm.password" placeholder="请输入密码" @keyup.enter="handleLogin">
            <span class="eye-icon" @click="togglePwd('loginPwd')">👁️</span>
          </div>
        </div>
        <p v-if="error" class="error-msg show">{{ error }}</p>
        <button class="btn" @click="handleLogin" :disabled="loading">
          {{ loading ? '登录中...' : '立即登录' }}
        </button>
      </div>
      <!-- 注册表单 -->
      <div class="auth-form" v-if="tab === 'register'">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <div class="input-wrapper">
            <span class="input-icon">👤</span>
            <input type="text" class="form-input" v-model="regForm.username" placeholder="请输入用户名">
          </div>
          <ul class="form-tip-list">
            <li>这是您的登录账号，请牢记！</li>
            <li>至少6个字符，只能用字母、数字</li>
          </ul>
        </div>
        <div class="form-group">
          <label class="form-label">密码</label>
          <div class="input-wrapper">
            <span class="input-icon">🔑</span>
            <input type="password" class="form-input" v-model="regForm.password" placeholder="请输入密码">
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">确认密码</label>
          <div class="input-wrapper">
            <span class="input-icon">🔑</span>
            <input type="password" class="form-input" v-model="regForm.confirmPassword" placeholder="请再次输入密码">
          </div>
        </div>
        <p v-if="error" class="error-msg show">{{ error }}</p>
        <button class="btn" @click="handleRegister" :disabled="loading">
          {{ loading ? '注册中...' : '立即注册' }}
        </button>
        <div class="form-footer">已有账号？<a @click="tab = 'login'">立即登录</a></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useAuthStore } from '../stores/auth.js'
import { useAppStore } from '../stores/app.js'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const appStore = useAppStore()
const router = useRouter()

const tab = ref('login')
const loading = ref(false)
const error = ref('')

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', confirmPassword: '' })

async function handleLogin() {
  error.value = ''
  if (!loginForm.username || !loginForm.password) { error.value = '请填写用户名和密码'; return }
  loading.value = true
  try {
    const data = await authStore.login(loginForm.username, loginForm.password)
    loading.value = false
    if (!data.activated) { router.push('/activate') }
    else { router.push('/home') }
  } catch (e) {
    loading.value = false
    error.value = e.response?.data?.error || e.message || '登录失败'
  }
}

async function handleRegister() {
  error.value = ''
  if (!regForm.username || !regForm.password || !regForm.confirmPassword) { error.value = '请填写所有字段'; return }
  if (regForm.password !== regForm.confirmPassword) { error.value = '两次密码不一致'; return }
  if (!/^[0-9a-zA-Z_]{6,}$/.test(regForm.username)) { error.value = '用户名至少6位，只能用字母、数字、下划线'; return }
  loading.value = true
  try {
    const data = await authStore.register(regForm.username, regForm.password, regForm.confirmPassword)
    await appStore.fetchScoreItems()
    loading.value = false
    router.push('/activate')
  } catch (e) {
    loading.value = false
    error.value = e.response?.data?.error || e.message || '注册失败'
  }
}

function togglePwd(id) {
  const el = document.getElementById(id)
  if (el) el.type = el.type === 'password' ? 'text' : 'password'
}
</script>

<style scoped>
.auth-page { display:flex; flex-direction:column; align-items:center; justify-content:center; min-height:100vh; padding:20px; }
.logo { width:60px; height:60px; background:#fff; border-radius:50%; display:flex; align-items:center; justify-content:center; margin-bottom:16px; font-size:32px; box-shadow:0 2px 8px rgba(0,0,0,0.05); }
.title { font-size:24px; font-weight:600; color:#2d3748; margin-bottom:8px; }
.subtitle { color:#666; margin-bottom:30px; font-size:14px; }
.auth-card { background:white; border-radius:16px; box-shadow:0 8px 24px rgba(0,0,0,0.1); width:100%; max-width:400px; overflow:hidden; }
.auth-tabs { display:flex; border-bottom:1px solid #eee; }
.auth-tab { flex:1; padding:16px; text-align:center; cursor:pointer; color:#666; font-weight:500; }
.auth-tab.active { color:#ff6b9d; position:relative; }
.auth-tab.active::after { content:""; position:absolute; bottom:-1px; left:0; width:100%; height:2px; background:#ff6b9d; }
.auth-form { padding:30px; }
.form-group { margin-bottom:20px; }
.form-label { display:block; margin-bottom:8px; color:#333; font-size:14px; font-weight:500; }
.input-wrapper { position:relative; }
.input-icon { position:absolute; left:12px; top:50%; transform:translateY(-50%); color:#a0aec0; }
.eye-icon { position:absolute; right:12px; top:50%; transform:translateY(-50%); cursor:pointer; color:#a0aec0; }
.form-input { width:100%; padding:12px 16px 12px 40px; border:1px solid #ddd; border-radius:8px; font-size:14px; }
.form-input:focus { outline:none; border-color:#ff6b9d; }
.btn { width:100%; padding:14px; background: linear-gradient(90deg, #f04e98 0%, #ed266e 100%); color:white; border:none; border-radius:8px; font-weight:500; cursor:pointer; font-size:15px; }
.btn:disabled { opacity:0.6; cursor:not-allowed; }
.form-tip-list { padding-left:20px; margin-top:6px; font-size:12px; color:#718096; }
.form-footer { margin-top:20px; text-align:center; font-size:14px; color:#666; }
.form-footer a { color:#ff6b9d; cursor:pointer; }
.error-msg { color:#ff4444; font-size:12px; margin-top:4px; display:none; }
.error-msg.show { display:block; }
</style>