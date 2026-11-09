<template>
  <div class="auth-page">
    <!-- ============ 左侧品牌区 ============ -->
    <section class="auth-art">
      <div class="blob b1"></div>
      <div class="blob b2"></div>
      <div class="blob b3"></div>

      <div class="art-logo">
        <div class="logo-mark">🐾</div>
        <div>
          <b>{{ systemName }}</b>
          <span>Class Pet Garden</span>
        </div>
      </div>

      <div class="art-copy">
        <h1>让每一次进步<br>都被<em>看见</em>。</h1>
        <p>把课堂表现变成宠物的口粮——答对一题、认真早读、作业优秀，孩子们的伙伴就会长大一点点。</p>
      </div>

      <div class="art-feats">
        <div class="feat"><i>⚡</i><b>一键加减分</b><span>课上一秒完成</span></div>
        <div class="feat"><i>🍖</i><b>宠物随表现成长</b><span>6 个等级形态</span></div>
        <div class="feat"><i>🏆</i><b>光荣榜投屏</b><span>全班一起看见</span></div>
      </div>

      <div class="art-pets">
        <div class="pet-bub">🐱</div>
        <div class="pet-bub">🐶</div>
        <div class="pet-bub">🐰</div>
        <div class="pet-bub">🦊</div>
      </div>

      <router-link to="/board" class="art-foot">
        <span>📺 教室投屏 / 家长会？</span>
        <b>打开公开看板 →</b>
      </router-link>
    </section>

    <!-- ============ 右侧表单区 ============ -->
    <section class="auth-side">
      <div class="auth-mobile-brand">
        <div class="logo-mark">🐾</div>
        <div>
          <b>{{ systemName }}</b>
          <span>让学习变得更有趣</span>
        </div>
      </div>

      <div class="auth-card">
        <div class="seg">
          <button :class="{ on: tab === 'login' }" @click="switchTab('login')">教师登录</button>
          <button :class="{ on: tab === 'register' }" @click="switchTab('register')">注册账号</button>
          <button :class="{ on: tab === 'student' }" @click="switchTab('student')">学生登录</button>
        </div>

        <div class="auth-h">{{ headline }}</div>
        <div class="auth-sub">{{ subline }}</div>

        <!-- ---------- 教师登录 ---------- -->
        <form v-if="tab === 'login'" class="auth-form" @submit.prevent="handleLogin">
          <div class="field">
            <label>用户名</label>
            <div class="wrap">
              <span class="ico">👤</span>
              <input type="text" v-model="loginForm.username" placeholder="请输入用户名" autocomplete="username">
            </div>
          </div>
          <div class="field">
            <label>密码</label>
            <div class="wrap">
              <span class="ico">🔒</span>
              <input :type="showPwd ? 'text' : 'password'" v-model="loginForm.password"
                     placeholder="请输入密码" autocomplete="current-password">
              <span class="eye" @click="showPwd = !showPwd">{{ showPwd ? '🙈' : '👁' }}</span>
            </div>
          </div>

          <p v-if="error" class="error-msg">{{ error }}</p>

          <button type="submit" class="auth-btn" :disabled="loading">
            {{ loading ? '登录中…' : '登录' }}
          </button>

          <div class="auth-alt">还没有账号？<a @click="switchTab('register')">注册后使用激活码开通 →</a></div>

          <div class="auth-hr">学生通道</div>
          <div class="auth-tip"><b>学号登录：</b>初始密码与学号一致，首次登录需修改密码后进入宠物小屋。</div>
        </form>

        <!-- ---------- 注册 ---------- -->
        <form v-else-if="tab === 'register'" class="auth-form" @submit.prevent="handleRegister">
          <div class="field">
            <label>用户名</label>
            <div class="wrap">
              <span class="ico">👤</span>
              <input type="text" v-model="regForm.username" placeholder="请输入用户名" autocomplete="username">
            </div>
            <div class="form-tip">至少 6 位，只能包含字母、数字、下划线；这是您的登录账号，请牢记。</div>
          </div>
          <div class="field">
            <label>密码</label>
            <div class="wrap">
              <span class="ico">🔒</span>
              <input :type="showPwd ? 'text' : 'password'" v-model="regForm.password"
                     placeholder="请输入密码" autocomplete="new-password">
              <span class="eye" @click="showPwd = !showPwd">{{ showPwd ? '🙈' : '👁' }}</span>
            </div>
          </div>
          <div class="field">
            <label>确认密码</label>
            <div class="wrap">
              <span class="ico">🔒</span>
              <input :type="showPwd ? 'text' : 'password'" v-model="regForm.confirmPassword"
                     placeholder="请再次输入密码" autocomplete="new-password">
            </div>
          </div>

          <p v-if="error" class="error-msg">{{ error }}</p>

          <button type="submit" class="auth-btn" :disabled="loading">
            {{ loading ? '注册中…' : '注册并继续' }}
          </button>

          <div class="auth-alt">已有账号？<a @click="switchTab('login')">立即登录</a></div>
        </form>

        <!-- ---------- 学生登录 ---------- -->
        <form v-else class="auth-form" @submit.prevent="handleStudentLogin">
          <div class="auth-tip"><b>学号登录：</b>输入老师给你的学号和密码（初始密码 = 学号）。</div>

          <div class="field">
            <label>学号</label>
            <div class="wrap">
              <span class="ico">🔢</span>
              <input type="text" v-model="studentForm.studentNo" placeholder="请输入学号，如 S0001">
            </div>
          </div>
          <div class="field">
            <label>密码</label>
            <div class="wrap">
              <span class="ico">🔒</span>
              <input :type="showPwd ? 'text' : 'password'" v-model="studentForm.password" placeholder="请输入你的密码">
              <span class="eye" @click="showPwd = !showPwd">{{ showPwd ? '🙈' : '👁' }}</span>
            </div>
          </div>

          <p v-if="error" class="error-msg">{{ error }}</p>

          <button type="submit" class="auth-btn student-btn" :disabled="loading">
            {{ loading ? '登录中…' : '🎓 学生登录' }}
          </button>

          <div class="auth-alt">我是老师，<a @click="switchTab('login')">返回教师登录</a></div>
        </form>
      </div>

      <div class="auth-copyright">ClassPet · 为课堂而生</div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth.js'
import { useAppStore } from '../stores/app.js'
import { useRouter } from 'vue-router'
import api from '../api/index.js'

const authStore = useAuthStore()
const appStore = useAppStore()
const router = useRouter()

const tab = ref('login')
const loading = ref(false)
const error = ref('')
const showPwd = ref(false)
const systemName = ref('课堂宠物乐园')

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', confirmPassword: '' })
const studentForm = reactive({ studentNo: '', password: '' })

const headline = computed(() => ({
  login: '欢迎回来 👋',
  register: '创建你的账号',
  student: '欢迎回教室 🎒'
}[tab.value]))

const subline = computed(() => ({
  login: '登录后管理你的班级与宠物乐园',
  register: '注册后需使用激活码开通完整功能',
  student: '用学号登录，去看看你的伙伴'
}[tab.value]))

function switchTab(t) {
  tab.value = t
  error.value = ''
  showPwd.value = false
}

onMounted(async () => {
  try {
    const { data } = await api.get('/settings')
    if (data.systemName) systemName.value = data.systemName
  } catch (e) {}
})

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
    await authStore.register(regForm.username, regForm.password, regForm.confirmPassword)
    await appStore.fetchScoreItems()
    loading.value = false
    router.push('/activate')
  } catch (e) {
    loading.value = false
    error.value = e.response?.data?.error || e.message || '注册失败'
  }
}

async function handleStudentLogin() {
  error.value = ''
  if (!studentForm.studentNo) { error.value = '请输入学号'; return }
  if (!studentForm.password) { error.value = '请输入密码'; return }
  loading.value = true
  try {
    const data = await authStore.studentLogin(studentForm.studentNo, studentForm.password)
    loading.value = false
    if (data.mustChangePassword) {
      router.push('/change-password')
    } else {
      router.push('/student-home')
    }
  } catch (e) {
    loading.value = false
    error.value = e.response?.data?.error || e.message || '登录失败'
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}

/* ============ 左侧品牌区 ============ */
.auth-art {
  position: relative;
  width: 46%;
  max-width: 660px;
  min-width: 460px;
  overflow: hidden;
  padding: 54px 56px 42px;
  display: flex;
  flex-direction: column;
  background: linear-gradient(158deg, #FFDDE9 0%, #F2E4FC 48%, #DEEBFD 100%);
  background: linear-gradient(158deg,
      color-mix(in srgb, var(--brand) 34%, #fff) 0%,
      color-mix(in srgb, var(--brand-2) 22%, #fff) 46%,
      color-mix(in srgb, var(--brand) 16%, #fff) 100%);
}
.auth-art::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 1;
  background-image: radial-gradient(rgba(255, 255, 255, .75) 1.2px, transparent 1.2px);
  background-size: 22px 22px;
  opacity: .5;
  pointer-events: none;
}
.auth-art::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    radial-gradient(560px 420px at 12% 8%, rgba(255, 255, 255, .85), transparent 70%),
    radial-gradient(420px 380px at 92% 92%, rgba(255, 255, 255, .55), transparent 72%);
  pointer-events: none;
}
.blob { position: absolute; border-radius: 50%; }
.b1 {
  width: 300px; height: 300px; left: -90px; top: 330px;
  background: radial-gradient(circle at 30% 30%, var(--brand-soft-2), transparent 70%);
}
.b2 {
  width: 340px; height: 340px; right: -110px; top: -90px;
  background: radial-gradient(circle at 60% 40%, var(--brand-soft), transparent 70%);
}
.b3 {
  width: 240px; height: 240px; right: 40px; bottom: 120px;
  background: radial-gradient(circle at 50% 50%, rgba(56, 189, 248, .16), transparent 70%);
}

.art-logo { position: relative; z-index: 2; display: flex; align-items: center; gap: 12px; }
.logo-mark {
  width: 46px; height: 46px;
  flex: 0 0 46px;
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 23px;
  background: linear-gradient(140deg, #fff, rgba(255, 255, 255, .6));
  box-shadow: 0 6px 16px var(--brand-soft-2), inset 0 0 0 1px rgba(255, 255, 255, .9);
}
.art-logo b { font-size: 16px; font-weight: 700; letter-spacing: -.01em; display: block; }
.art-logo span { font-size: 11.5px; color: #8A8394; }

.art-copy { position: relative; z-index: 2; margin-top: 66px; }
.art-copy h1 {
  font-size: 40px;
  line-height: 1.26;
  letter-spacing: -.03em;
  font-weight: 700;
  color: #2A2333;
}
.art-copy h1 em { font-style: normal; position: relative; color: var(--brand-deep); }
.art-copy h1 em::after {
  content: "";
  position: absolute;
  left: -2px; right: -2px; bottom: 1px;
  height: 10px;
  border-radius: 6px;
  background: var(--brand-soft-2);
  z-index: -1;
}
.art-copy p {
  margin-top: 16px;
  font-size: 14.5px;
  line-height: 1.85;
  color: #6E6675;
  max-width: 390px;
}

.art-feats { position: relative; z-index: 2; display: flex; flex-direction: column; gap: 11px; margin-top: 32px; }
.feat {
  display: flex;
  align-items: center;
  gap: 11px;
  background: rgba(255, 255, 255, .62);
  border: 1px solid rgba(255, 255, 255, .9);
  border-radius: 13px;
  padding: 10px 14px;
  width: fit-content;
  box-shadow: 0 4px 14px rgba(60, 40, 80, .05);
  backdrop-filter: blur(6px);
}
.feat i { font-style: normal; font-size: 15px; }
.feat b { font-size: 13px; font-weight: 600; color: #3C3446; }
.feat span { font-size: 12px; color: #8A8394; margin-left: 2px; }

.art-pets { position: relative; z-index: 2; margin-top: auto; padding-top: 40px; display: flex; align-items: flex-end; gap: 12px; }
.pet-bub {
  width: 76px; height: 76px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  background: rgba(255, 255, 255, .78);
  box-shadow: 0 10px 24px rgba(80, 50, 90, .12), inset 0 0 0 1px rgba(255, 255, 255, .9);
  animation: float 5s ease-in-out infinite;
}
.pet-bub:nth-child(2) { width: 96px; height: 96px; font-size: 46px; animation-delay: -1.4s; margin-bottom: 14px; }
.pet-bub:nth-child(3) { animation-delay: -2.6s; }
.pet-bub:nth-child(4) { width: 64px; height: 64px; font-size: 30px; animation-delay: -3.6s; margin-bottom: 6px; }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-9px); } }

.art-foot {
  position: relative;
  z-index: 2;
  margin-top: 26px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #8A8394;
  text-decoration: none;
}
.art-foot b { color: var(--brand-deep); font-weight: 600; }
.art-foot:hover b { text-decoration: underline; }

/* ============ 右侧表单区 ============ */
.auth-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  position: relative;
}
.auth-side::before {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(700px 400px at 110% 110%, var(--brand-soft), transparent 70%);
  pointer-events: none;
}
.auth-mobile-brand { display: none; }

.auth-card { position: relative; width: 100%; max-width: 392px; }

.seg { display: flex; background: #F4EFEE; padding: 4px; border-radius: 12px; margin-bottom: 26px; }
.seg button {
  flex: 1;
  border: 0;
  background: transparent;
  padding: 9px 0;
  border-radius: 9px;
  font-size: 13px;
  color: #767D8C;
  cursor: pointer;
  font-weight: 500;
  transition: .18s;
}
.seg button.on { background: #fff; color: var(--ink); font-weight: 600; box-shadow: 0 1px 3px rgba(0, 0, 0, .07); }

.auth-h { font-size: 24px; font-weight: 700; letter-spacing: -.02em; margin-bottom: 6px; }
.auth-sub { font-size: 13px; color: var(--ink-3); margin-bottom: 22px; }

.auth-form { display: block; }
.field { margin-bottom: 15px; }
.field label { display: block; font-size: 12.5px; font-weight: 600; color: #4A5160; margin-bottom: 7px; }
.field .wrap { position: relative; }
.field input {
  width: 100%;
  height: 46px;
  border: 1.5px solid #EDE6E4;
  background: #fff;
  border-radius: 12px;
  padding: 0 42px 0 42px;
  font-size: 14px;
  font-family: inherit;
  color: var(--ink);
  transition: .18s;
}
.field input::placeholder { color: #BCC1CB; }
.field input:focus { outline: 0; border-color: var(--brand); box-shadow: 0 0 0 4px var(--brand-soft); }
.field .ico { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); font-size: 14px; opacity: .55; }
.field .eye {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 13px;
  opacity: .5;
  cursor: pointer;
  user-select: none;
}
.field .eye:hover { opacity: .85; }
.form-tip { font-size: 11.5px; color: var(--ink-3); margin-top: 6px; line-height: 1.6; }

.error-msg {
  font-size: 12.5px;
  color: #C0392B;
  background: #FDF1EF;
  border: 1px solid #F8DBD6;
  border-radius: 9px;
  padding: 8px 11px;
  margin-bottom: 12px;
}

.auth-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  border: 0;
  cursor: pointer;
  font-size: 14.5px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, var(--brand) 0%, var(--brand-deep) 100%);
  box-shadow: 0 10px 24px var(--brand-soft-2);
  transition: transform .2s, box-shadow .2s;
}
.auth-btn:hover { transform: translateY(-1px); box-shadow: 0 14px 30px var(--brand-soft-2); }
.auth-btn:disabled { opacity: .65; cursor: not-allowed; transform: none; }
.student-btn { background: linear-gradient(135deg, var(--brand-2) 0%, var(--brand) 120%); }

.auth-alt { margin-top: 16px; text-align: center; font-size: 12.5px; color: var(--ink-3); }
.auth-alt a { color: var(--brand-deep); font-weight: 600; cursor: pointer; }
.auth-alt a:hover { text-decoration: underline; }

.auth-hr { display: flex; align-items: center; gap: 12px; margin: 24px 0 14px; font-size: 11px; color: #C3C7D0; }
.auth-hr::before, .auth-hr::after { content: ""; flex: 1; height: 1px; background: var(--line); }

.auth-tip {
  background: var(--brand-soft);
  border: 1px solid var(--brand-soft-2);
  border-radius: 11px;
  padding: 11px 13px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--brand-deep);
  margin-bottom: 14px;
}
.auth-tip b { font-weight: 700; }

.auth-copyright {
  position: relative;
  margin-top: 34px;
  font-size: 11px;
  color: var(--ink-4);
}

/* ============ 响应式 ============ */
@media (max-width: 1024px) {
  .auth-art { min-width: 400px; padding: 42px 40px 34px; }
  .art-copy { margin-top: 46px; }
  .art-copy h1 { font-size: 32px; }
  .art-feats { margin-top: 26px; }
  .feat span { display: none; }
}

@media (max-width: 860px) {
  .auth-page { flex-direction: column; background: var(--bg); }
  .auth-art { display: none; }
  .auth-side { padding: 26px 20px 40px; justify-content: flex-start; }
  .auth-mobile-brand {
    display: flex;
    align-items: center;
    gap: 11px;
    width: 100%;
    max-width: 392px;
    margin: 4px 0 22px;
  }
  .auth-mobile-brand .logo-mark { width: 40px; height: 40px; flex: 0 0 40px; border-radius: 13px; font-size: 20px; }
  .auth-mobile-brand b { display: block; font-size: 15px; font-weight: 700; }
  .auth-mobile-brand span { font-size: 11.5px; color: var(--ink-3); }
  .auth-h { font-size: 21px; }
  .auth-copyright { margin-top: auto; padding-top: 30px; }
}
</style>
