<template>
  <div class="activate-page">
    <div class="lock-icon">🔒</div>
    <h1 class="page-title">激活您的账号</h1>
    <div class="activate-card">
      <div class="user-info">
        <div class="avatar">{{ authStore.user?.username?.charAt(0).toUpperCase() || 'U' }}</div>
        <div class="user-details">
          <div class="username">{{ authStore.user?.username }}</div>
          <div class="user-status">账号未激活</div>
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">激活码</label>
        <div class="input-wrapper">
          <span class="input-icon">🔑</span>
          <input type="text" class="form-input" v-model="code" placeholder="memory_clear" @keyup.enter="handleActivate" style="text-transform:lowercase;">
        </div>
      </div>
      <p v-if="error" class="error-msg show">{{ error }}</p>
      <div class="form-tip">💡 输入激活码：memory_clear</div>
      <button class="btn btn-primary" @click="handleActivate" :disabled="loading">
        {{ loading ? '激活中...' : '立即激活' }}
      </button>
      <div class="divider">还没有激活码？</div>
      <button class="btn-outline" @click="code = 'memory_clear'">
        🖱️ 点击这里激活
      </button>
      <div v-if="showDemoTip" class="demo-tip">
        📝 请输入激活码：<code>memory_clear</code>
      </div>
      <div class="logout-link" @click="handleLogout">↩️ 退出登录</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth.js'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()
const code = ref('')
const error = ref('')
const loading = ref(false)
const showDemoTip = ref(false)

async function handleActivate() {
  error.value = ''
  if (!code.value.trim()) { error.value = '请输入激活码'; return }
  loading.value = true
  try {
    await authStore.activate(code.value.trim())
    loading.value = false
    router.push('/home')
  } catch (e) {
    loading.value = false
    error.value = e.response?.data?.error || e.message || '激活失败'
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<style scoped>
.activate-page { display:flex; flex-direction:column; align-items:center; justify-content:center; min-height:100vh; padding:20px; background: linear-gradient(135deg, #fff8e8 0%, #ffeef0 100%); }
.lock-icon { width:60px; height:60px; background:#fff; border-radius:50%; display:flex; align-items:center; justify-content:center; margin-bottom:16px; font-size:28px; box-shadow:0 4px 12px rgba(0,0,0,0.05); }
.page-title { font-size:24px; font-weight:600; color:#2d3748; margin-bottom:24px; }
.activate-card { background:white; border-radius:16px; box-shadow:0 8px 24px rgba(0,0,0,0.1); width:100%; max-width:400px; padding:30px; text-align:center; }
.user-info { display:flex; align-items:center; gap:12px; padding:12px; background:#f7f9fc; border-radius:12px; margin-bottom:24px; text-align:left; }
.avatar { width:40px; height:40px; border-radius:50%; background:#ff6b9d; color:white; display:flex; align-items:center; justify-content:center; font-weight:500; font-size:18px; }
.username { font-size:16px; font-weight:500; color:#2d3748; }
.user-status { font-size:12px; color:#718096; }
.form-group { margin-bottom:16px; text-align:left; }
.form-label { display:block; margin-bottom:8px; font-weight:500; }
.input-wrapper { position:relative; }
.input-icon { position:absolute; left:12px; top:50%; transform:translateY(-50%); }
.form-input { width:100%; padding:12px 16px 12px 40px; border:1px solid #ddd; border-radius:8px; font-size:16px; letter-spacing:2px; }
.btn { width:100%; padding:14px; color:white; border:none; border-radius:8px; font-size:15px; cursor:pointer; font-weight:500; }
.btn-primary { background: linear-gradient(90deg, #ffc168 0%, #ff9f69 100%); }
.btn:disabled { opacity:0.6; cursor:not-allowed; }
.error-msg { color:#ff4444; font-size:12px; display:none; text-align:left; }
.error-msg.show { display:block; }
.form-tip { font-size:12px; color:#999; margin-top:8px; text-align:left; }
.divider { display:flex; align-items:center; gap:8px; margin:20px 0; color:#718096; font-size:13px; }
.divider::before, .divider::after { content:""; flex:1; height:1px; background:#e2e8f0; }
.btn-outline { width:100%; padding:12px; background:#fff; border:1px solid #ffc168; border-radius:8px; color:#b87323; font-size:15px; font-weight:500; cursor:pointer; display:flex; align-items:center; justify-content:center; gap:4px; }
.demo-tip { margin-top:12px; font-size:12px; color:#666; background:#fffbea; border-radius:6px; padding:12px; text-align:left; line-height:1.6; }
.demo-tip code { background:#fff3cd; padding:2px 6px; border-radius:4px; font-size:14px; }
.logout-link { display:flex; align-items:center; justify-content:center; gap:4px; margin-top:24px; font-size:14px; color:#718096; cursor:pointer; }
</style>