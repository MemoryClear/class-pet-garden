<template>
  <div class="settings-page">
    <div class="header-bar">
      <button class="back-btn" @click="router.push('/home')">← 返回</button>
      <h2>⚙️ 系统设置</h2>
    </div>
    <div class="container">
      <!-- 班级信息 -->
      <div class="section-card">
        <div class="section-title">📋 班级信息</div>
        <div class="form-group">
          <label class="form-label">系统名称</label>
          <input type="text" class="form-input" v-model="form.systemName" @change="save" placeholder="如：课堂宠物乐园">
        </div>
        <div class="form-group">
          <label class="form-label">班级名称</label>
          <input type="text" class="form-input" v-model="form.className" @change="save" placeholder="如：初一(3)班">
        </div>
      </div>

      <!-- 主题选择 -->
      <div class="section-card">
        <div class="section-title">🎨 主题颜色</div>
        <div class="theme-grid">
          <div v-for="t in themes" :key="t.value" class="theme-item"
               :style="{background: t.bg}"
               :class="{selected: form.theme === t.value}"
               @click="selectTheme(t.value)">
            <span class="theme-emoji">{{ t.emoji }}</span>
          </div>
        </div>
      </div>

      <!-- 学生管理 -->
      <div class="section-card">
        <div class="section-title">👥 学生管理</div>
        <!-- 添加单个 -->
        <div class="form-group">
          <label class="form-label">添加单个学生</label>
          <div class="add-row">
            <input type="text" class="form-input" v-model="newName" placeholder="输入姓名" @keyup.enter="addSingle">
            <button class="btn-sm" @click="addSingle">添加</button>
          </div>
        </div>
        <!-- 批量导入 -->
        <div class="form-group">
          <label class="form-label">批量导入（每行一个姓名）</label>
          <textarea class="form-textarea" v-model="batchNames" placeholder="张三&#10;李四&#10;王五"></textarea>
          <button class="btn-sm" @click="addBatch" :disabled="appStore.loading">
            {{ appStore.loading ? '导入中...' : '批量导入' }}
          </button>
        </div>
        <p v-if="manageError" class="error-msg show">{{ manageError }}</p>
        <!-- 学生列表 -->
        <div class="student-list" v-if="appStore.students.length > 0">
          <div v-for="stu in appStore.students" :key="stu.id" class="student-item">
            <span>{{ stu.name }}</span>
            <div class="student-actions">
              <button class="icon-btn" @click="promptEdit(stu)" title="修改">✏️</button>
              <button class="icon-btn" @click="deleteStudent(stu.id)" title="删除">🗑️</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 评分项管理 -->
      <div class="section-card">
        <div class="section-title">🏷️ 评分项管理</div>
        <div class="score-item-list">
          <div v-for="item in appStore.scoreItems" :key="item.id" class="score-item">
            <span class="score-icon">{{ item.icon }}</span>
            <span class="score-name">{{ item.name }}</span>
            <span class="score-pt" :class="item.point > 0 ? 'pos' : 'neg'">
              {{ item.point > 0 ? '+' : '' }}{{ item.point }}
            </span>
            <button class="icon-btn" @click="deleteScoreItem(item.id)">🗑️</button>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">添加评分项</label>
          <div class="add-score-row">
            <input type="text" class="form-input small" v-model="scoreForm.icon" placeholder="emoji" maxlength="2">
            <input type="text" class="form-input" v-model="scoreForm.name" placeholder="名称" style="flex:1">
            <input type="number" class="form-input small" v-model="scoreForm.point" placeholder="分值" style="width:60px">
            <button class="btn-sm" @click="addScoreItem">添加</button>
          </div>
        </div>
      </div>

      <!-- 退出 -->
      <div class="section-card">
        <button class="btn-logout" @click="handleLogout">↩️ 退出登录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'
import { useAuthStore } from '../stores/auth.js'
import { useRouter } from 'vue-router'

const appStore = useAppStore()
const authStore = useAuthStore()
const router = useRouter()

const form = reactive({ systemName: '', className: '', theme: 'pink' })
const newName = ref('')
const batchNames = ref('')
const manageError = ref('')
const scoreForm = reactive({ icon: '', name: '', point: null })

const themes = [
  { value: 'pink', bg: '#fff0f3', emoji: '🌸' },
  { value: 'purple', bg: '#f3f0ff', emoji: '💜' },
  { value: 'indigo', bg: '#ede9fe', emoji: '🌟' },
  { value: 'blue', bg: '#dbeafe', emoji: '🌊' },
  { value: 'cyan', bg: '#cffafe', emoji: '🐳' },
  { value: 'teal', bg: '#ccfbf1', emoji: '🌿' },
  { value: 'green', bg: '#dcfce7', emoji: '🍀' },
  { value: 'yellow', bg: '#fef9c3', emoji: '🌻' },
  { value: 'orange', bg: '#ffedd5', emoji: '🍊' },
  { value: 'red', bg: '#fee2e2', emoji: '❤️' },
]

async function save() {
  await appStore.updateSettings({ ...form })
}

async function selectTheme(t) {
  form.theme = t
  await save()
}

async function addSingle() {
  manageError.value = ''
  if (!newName.value.trim()) return
  try {
    await appStore.createStudent(newName.value.trim())
    newName.value = ''
  } catch (e) { manageError.value = e.response?.data?.error || e.message }
}

async function addBatch() {
  manageError.value = ''
  if (!batchNames.value.trim()) return
  try {
    await appStore.batchCreateStudents(batchNames.value)
    batchNames.value = ''
    manageError.value = ''
  } catch (e) { manageError.value = e.response?.data?.error || e.message }
}

async function deleteStudent(id) {
  if (!confirm('确认删除？')) return
  try { await appStore.deleteStudent(id) } catch (e) { manageError.value = e.message }
}

function promptEdit(stu) {
  const name = prompt('修改学生姓名', stu.name)
  if (name && name.trim() && name !== stu.name) {
    appStore.updateStudent(stu.id, name.trim()).catch(e => { manageError.value = e.message })
  }
}

async function deleteScoreItem(id) {
  await appStore.deleteScoreItem(id)
}

async function addScoreItem() {
  if (!scoreForm.icon || !scoreForm.name || scoreForm.point == null) return
  await appStore.createScoreItem(scoreForm.icon, scoreForm.name, Number(scoreForm.point))
  scoreForm.icon = ''
  scoreForm.name = ''
  scoreForm.point = null
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

onMounted(async () => {
  await appStore.fetchStudents()
  await appStore.fetchScoreItems()
  await appStore.fetchSettings()
  Object.assign(form, {
    systemName: appStore.settings.systemName || '',
    className: appStore.settings.className || '',
    theme: appStore.settings.theme || 'pink'
  })
})
</script>

<style scoped>
.settings-page { min-height:100vh; }
.header-bar { background:#fff; padding:16px 20px; display:flex; align-items:center; gap:16px; box-shadow:0 2px 8px rgba(0,0,0,0.06); margin-bottom:20px; }
.back-btn { background:#f0f0f0; border:none; padding:8px 16px; border-radius:8px; cursor:pointer; font-size:14px; }
h2 { font-size:18px; font-weight:600; color:#2d3748; }
.container { max-width:800px; margin:0 auto; padding:0 20px 40px; display:flex; flex-direction:column; gap:16px; }
.section-card { background:#fff; border-radius:16px; box-shadow:0 2px 8px rgba(0,0,0,0.06); padding:20px; }
.section-title { font-size:16px; font-weight:600; color:#2d3748; margin-bottom:16px; }
.form-group { margin-bottom:12px; }
.form-label { display:block; font-size:13px; color:#718096; margin-bottom:6px; }
.form-input { width:100%; padding:10px 12px; border:1px solid #e2e8f0; border-radius:8px; font-size:14px; }
.form-input.small { width:60px; }
.form-textarea { width:100%; padding:10px 12px; border:1px solid #e2e8f0; border-radius:8px; font-size:14px; min-height:100px; resize:vertical; margin-bottom:8px; }
.add-row { display:flex; gap:8px; }
.btn-sm { background:#ff4d79; color:#fff; border:none; padding:10px 16px; border-radius:8px; cursor:pointer; font-size:14px; white-space:nowrap; }
.add-score-row { display:flex; gap:8px; align-items:center; }
.theme-grid { display:grid; grid-template-columns:repeat(5, 1fr); gap:8px; }
.theme-item { height:56px; border-radius:12px; display:flex; align-items:center; justify-content:center; cursor:pointer; border:2px solid transparent; transition:all 0.2s; }
.theme-item.selected { border-color:#ff4d79; }
.theme-emoji { font-size:24px; }
.student-list { margin-top:12px; display:flex; flex-wrap:wrap; gap:8px; }
.student-item { background:#f7f3f0; border-radius:8px; padding:6px 12px; display:flex; align-items:center; gap:8px; font-size:14px; }
.student-actions { display:flex; gap:4px; }
.icon-btn { background:transparent; border:none; cursor:pointer; font-size:14px; }
.error-msg { color:#ff4444; font-size:12px; display:none; }
.error-msg.show { display:block; }
.score-item-list { display:flex; flex-direction:column; gap:8px; margin-bottom:12px; }
.score-item { display:flex; align-items:center; gap:8px; padding:8px 12px; background:#f7fafc; border-radius:8px; }
.score-icon { font-size:20px; }
.score-name { flex:1; }
.score-pt { font-weight:600; }
.score-pt.pos { color:#52c41a; }
.score-pt.neg { color:#ef4444; }
.btn-logout { width:100%; padding:12px; background:#fff; border:1px solid #ff4444; border-radius:8px; color:#ff4444; cursor:pointer; font-size:14px; }
</style>