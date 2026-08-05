<template>
  <div class="auth-page">
    <div class="logo">🔑</div>
    <h1 class="title">修改密码</h1>
    <p class="subtitle" v-if="auth.mustChangePassword">首次登录请修改你的密码</p>
    <p class="subtitle" v-else>输入新密码并确认</p>
    <div class="auth-card">
      <div class="auth-form">
        <div class="form-group">
          <label class="form-label">新密码</label>
          <div class="input-wrapper">
            <span class="input-icon">🔒</span>
            <input
              :type="showPwd ? 'text' : 'password'"
              class="form-input"
              v-model="newPassword"
              placeholder="至少 4 个字符"
              @keyup.enter="handleSubmit"
            >
            <button class="toggle-eye" type="button" @click="showPwd = !showPwd">
              {{ showPwd ? '🙈' : '👁️' }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">确认密码</label>
          <div class="input-wrapper">
            <span class="input-icon">🔒</span>
            <input
              :type="showPwd ? 'text' : 'password'"
              class="form-input"
              v-model="confirmPassword"
              placeholder="再输入一次"
              @keyup.enter="handleSubmit"
            >
          </div>
        </div>
        <p v-if="error" class="error-msg show">{{ error }}</p>
        <p v-if="hint" class="hint-msg show">{{ hint }}</p>
        <button class="btn" @click="handleSubmit" :disabled="loading">
          {{ loading ? '提交中...' : '确认修改' }}
        </button>
        <div class="form-footer" v-if="!auth.mustChangePassword">
          <a @click="goBack">← 返回</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const auth = useAuthStore()
const newPassword = ref('')
const confirmPassword = ref('')
const showPwd = ref(false)
const loading = ref(false)
const error = ref('')
const hint = ref('')

async function handleSubmit() {
  error.value = ''
  hint.value = ''
  if (!newPassword.value) { error.value = '请输入新密码'; return }
  if (newPassword.value.length < 4) { error.value = '新密码至少 4 个字符'; return }
  if (newPassword.value !== confirmPassword.value) { error.value = '两次密码不一致'; return }
  loading.value = true
  try {
    await auth.studentChangePassword(newPassword.value)
    loading.value = false
    hint.value = '密码修改成功！'
    setTimeout(() => {
      router.push(auth.isStudent ? '/student-home' : '/home')
    }, 600)
  } catch (e) {
    loading.value = false
    error.value = e.response?.data?.error || e.message || '修改失败'
  }
}

function goBack() {
  router.push(auth.isStudent ? '/student-home' : '/home')
}
</script>

<style scoped>
.auth-page { display:flex; flex-direction:column; align-items:center; justify-content:center; min-height:100vh; padding:20px; }
.logo { width:60px; height:60px; background:#fff; border-radius:50%; display:flex; align-items:center; justify-content:center; margin-bottom:16px; font-size:32px; box-shadow:0 2px 8px rgba(0,0,0,0.05); }
.title { font-size:24px; font-weight:600; color:#2d3748; margin-bottom:8px; }
.subtitle { color:#666; margin-bottom:30px; font-size:14px; }
.auth-card { background:white; border-radius:16px; box-shadow:0 8px 24px rgba(0,0,0,0.1); width:100%; max-width:400px; overflow:hidden; }
.auth-form { padding:30px; }
.form-group { margin-bottom:20px; }
.form-label { display:block; margin-bottom:8px; color:#333; font-size:14px; font-weight:500; }
.input-wrapper { position:relative; }
.input-icon { position:absolute; left:12px; top:50%; transform:translateY(-50%); color:#a0aec0; }
.form-input { width:100%; padding:12px 40px 12px 40px; border:1px solid #ddd; border-radius:8px; font-size:14px; box-sizing:border-box; }
.form-input:focus { outline:none; border-color:#ff6b9d; }
.toggle-eye { position:absolute; right:8px; top:50%; transform:translateY(-50%); background:transparent; border:none; cursor:pointer; padding:4px; font-size:16px; }
.btn { width:100%; padding:14px; background: linear-gradient(90deg, #f04e98 0%, #ed266e 100%); color:white; border:none; border-radius:8px; font-weight:500; cursor:pointer; font-size:15px; }
.btn:disabled { opacity:0.6; cursor:not-allowed; }
.error-msg { color:#ff4444; font-size:12px; margin-top:4px; display:none; }
.error-msg.show { display:block; }
.hint-msg { color:#52c41a; font-size:13px; margin-top:4px; display:none; }
.hint-msg.show { display:block; }
.form-footer { margin-top:20px; text-align:center; font-size:14px; color:#666; }
.form-footer a { color:#ff6b9d; cursor:pointer; }
</style>