<template>
  <AppShell title="系统设置" subtitle="班级信息、外观、主题与评分项">
    <div class="container">
      <!-- 班级信息 -->
      <div class="section-card">
        <div class="section-title">📋 班级信息</div>
        <div class="form-group">
          <label class="form-label">系统名称</label>
          <input type="text" class="form-input" v-model="form.systemName" @change="save" placeholder="如：课堂宠物乐园">
        </div>
        <div class="form-group">
          <label class="form-label">当前班级名称</label>
          <input type="text" class="form-input" v-model="clsForm.name" @change="renameCurrentClass" placeholder="如：初一(3)班">
          <div class="form-hint">
            班级号 <b>{{ currentClass?.code || '—' }}</b>，学生学号为「班级号-S0001」；
            改这里只影响当前选中的班级。
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">公开看板</label>
          <div style="display:flex; align-items:center; gap:12px;">
            <label class="switch">
              <input type="checkbox" v-model="clsForm.showOnBoard" @change="saveBoard">
              <span class="slider"></span>
            </label>
            <span style="font-size:13px; color:var(--ink-3);">
              开启后，「{{ currentClass?.name || '当前班级' }}」会出现在无需登录的公开看板中
            </span>
          </div>
        </div>
      </div>

      <!-- 班级管理 -->
      <div class="section-card">
        <div class="section-title">🏫 班级管理</div>
        <div class="class-list" v-if="appStore.classes.length">
          <div
            v-for="c in appStore.classes"
            :key="c.id"
            class="class-row"
            :class="{ on: c.id === appStore.currentClassId }"
          >
            <div class="class-row-main">
              <b>{{ c.name }}</b>
              <span>{{ c.code }} · {{ c.studentCount ?? 0 }} 人</span>
            </div>
            <span v-if="c.id === appStore.currentClassId" class="class-tag">当前</span>
            <div class="class-row-actions">
              <button class="icon-btn" title="重命名" @click="renameClass(c)">✏️</button>
              <button class="icon-btn" title="删除班级" @click="deleteClass(c)">🗑️</button>
            </div>
          </div>
        </div>
        <div class="add-row" style="margin-top:12px;">
          <input class="form-input" v-model="newClassName" placeholder="新班级名称，如 初二(1)班" @keyup.enter="createClass">
          <button class="btn-sm" @click="createClass">＋ 新建班级</button>
        </div>
        <div class="form-hint">学生、加减分历史、兑换记录、光荣榜都按班级隔离；评分项与商品是全教师共享的。</div>
      </div>

      <!-- 外观模式 -->
      <div class="section-card">
        <div class="section-title">🌗 外观模式</div>
        <div class="mode-row">
          <button class="mode-btn" :class="{ active: form.themeMode === 'light' }" @click="selectMode('light')">
            <span class="mode-ico">☀️</span>
            <span class="mode-txt"><b>亮色</b><i>白天 / 投屏更清爽</i></span>
          </button>
          <button class="mode-btn" :class="{ active: form.themeMode === 'dark' }" @click="selectMode('dark')">
            <span class="mode-ico">🌙</span>
            <span class="mode-txt"><b>深色</b><i>傍晚 / 关灯更护眼</i></span>
          </button>
        </div>
        <div class="form-hint">外观偏好保存在账号里，换设备登录也会保留</div>
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

      <!-- 学生管理入口 -->
      <div class="section-card">
        <div class="section-title">👥 学生管理</div>
        <p class="section-desc">
          学生名单、学号与密码已移到独立页面：添加、批量导入、重置密码、随机领养都在那里完成。
        </p>
        <button class="btn-sm goto-students" @click="router.push('/students')">前往学生管理 →</button>
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
  </AppShell>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'
import { useAuthStore } from '../stores/auth.js'
import { useRouter } from 'vue-router'
import $confirm from '../composables/useConfirmModal.js'
import AppShell from '../components/layout/AppShell.vue'


const appStore = useAppStore()
const authStore = useAuthStore()
const router = useRouter()

const form = reactive({ systemName: '', theme: 'pink', themeMode: 'light' })
const clsForm = reactive({ name: '', showOnBoard: false })
const newClassName = ref('')
const currentClass = computed(() => appStore.currentClass)
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

// ---------- 班级 ----------
async function renameCurrentClass() {
  const id = appStore.currentClassId
  const name = (clsForm.name || '').trim()
  if (!id || !name) return
  try {
    const { classApi } = await import('../api/index.js')
    await classApi.rename(id, name)
    await appStore.fetchClasses()
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
    clsForm.name = currentClass.value?.name || ''
  }
}

async function saveBoard() {
  const id = appStore.currentClassId
  if (!id) return
  try {
    const { classApi } = await import('../api/index.js')
    await classApi.setBoard(id, clsForm.showOnBoard)
    await appStore.fetchClasses()
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
    clsForm.showOnBoard = !clsForm.showOnBoard
  }
}

async function createClass() {
  const name = (newClassName.value || '').trim()
  if (!name) return
  try {
    const { classApi } = await import('../api/index.js')
    const { data } = await classApi.create(name)
    newClassName.value = ''
    await appStore.fetchClasses()
    await appStore.switchClass(data.id)
    syncClassForm()
    $confirm.success(`已创建「${data.name}」，班级号 ${data.code}`)
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
  }
}

async function renameClass(c) {
  const name = await $confirm.prompt(`修改「${c.name}」的名称：`, {
    title: '重命名班级',
    inputValue: c.name,
    inputRequired: true,
    okText: '保存'
  })
  if (name === null || !name.trim()) return
  try {
    const { classApi } = await import('../api/index.js')
    await classApi.rename(c.id, name.trim())
    await appStore.fetchClasses()
    if (c.id === appStore.currentClassId) syncClassForm()
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
  }
}

async function deleteClass(c) {
  const ok = await $confirm.confirm(
    `确定删除「${c.name}」吗？只有空班级可以删除，删除后该班级号不再使用。`,
    { title: '删除班级', okText: '删除', icon: '🗑️', danger: true }
  )
  if (!ok) return
  try {
    const { classApi } = await import('../api/index.js')
    await classApi.remove(c.id)
    await appStore.fetchClasses()
    await appStore.fetchStudents()
    syncClassForm()
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
  }
}

function syncClassForm() {
  const c = appStore.currentClass
  clsForm.name = c?.name || ''
  clsForm.showOnBoard = !!c?.showOnBoard
}

watch(() => appStore.currentClassId, syncClassForm)

async function selectMode(m) {
  if (form.themeMode === m) return
  form.themeMode = m
  await save()
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



// 侧栏快捷切换外观后，这里的选中态要跟上
watch(() => appStore.settings.themeMode, (m) => {
  form.themeMode = m === 'dark' ? 'dark' : 'light'
})
watch(() => appStore.settings.theme, (t) => {
  if (t) form.theme = t
})

onMounted(async () => {
  await appStore.fetchStudents()
  await appStore.fetchScoreItems()
  await appStore.fetchSettings()
  Object.assign(form, {
    systemName: appStore.settings.systemName || '',
    theme: appStore.settings.theme || 'pink',
    themeMode: appStore.settings.themeMode === 'dark' ? 'dark' : 'light'
  })
  await appStore.fetchClasses()
  syncClassForm()
})
</script>

<style scoped>
.container { max-width: 820px; margin: 0 auto; display: flex; flex-direction: column; gap: 14px; }
.section-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-s);
  padding: 18px 20px;
}
.section-title { font-size: 15px; font-weight: 700; color: var(--ink); margin-bottom: 14px; }
.section-desc { font-size: 12.5px; line-height: 1.7; color: var(--ink-3); margin-bottom: 14px; }
.goto-students { width: auto; }

/* 外观模式 */
.mode-row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 10px; }
.mode-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1.5px solid var(--line);
  border-radius: var(--r-sm);
  background: var(--surface);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: .16s;
  touch-action: manipulation;
}
.mode-btn:hover { border-color: #E2D8D5; }
.mode-btn.active {
  border-color: var(--brand);
  background: var(--brand-soft);
}
.mode-ico { font-size: 20px; line-height: 1; }
.mode-txt { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.mode-txt b { font-size: 13.5px; font-weight: 600; color: var(--ink); }
.mode-txt i { font-style: normal; font-size: 11.5px; color: var(--ink-3); }
.form-hint { font-size: 11.5px; color: var(--ink-3); margin-top: 4px; line-height: 1.7; }
.form-hint b { color: var(--brand-deep); }

/* 班级列表 */
.class-list { display: flex; flex-direction: column; gap: 8px; }
.class-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  background: var(--surface);
}
.class-row.on { border-color: var(--brand-soft-2); background: var(--brand-soft); }
.class-row-main { flex: 1; min-width: 0; }
.class-row-main b { display: block; font-size: 13.5px; font-weight: 600; color: var(--ink); }
.class-row-main span { font-size: 11.5px; color: var(--ink-3); }
.class-tag {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--brand-deep);
  background: var(--brand-soft-2);
  padding: 2px 7px;
  border-radius: 6px;
}
.class-row-actions { display: flex; gap: 6px; }
.form-group { margin-bottom:12px; }
.form-label { display:block; font-size:13px; color:#718096; margin-bottom:6px; }
.form-input { width:100%; padding:10px 12px; border:1px solid #e2e8f0; border-radius:8px; font-size:14px; }
.form-input.small { width:60px; }
.form-input.small-input { width:180px; }
.form-textarea { width:100%; padding:10px 12px; border:1px solid #e2e8f0; border-radius:8px; font-size:14px; min-height:100px; resize:vertical; margin-bottom:8px; }
.form-hint { color:#718096; font-size:12px; margin-top:6px; }
.add-row { display:flex; gap:8px; }
.btn-sm { background:#ff4d79; color:#fff; border:none; padding:10px 16px; border-radius:8px; cursor:pointer; font-size:14px; white-space:nowrap; }
.add-score-row { display:flex; gap:8px; align-items:center; }
.theme-grid { display:grid; grid-template-columns:repeat(5, 1fr); gap:8px; }
.theme-item { height:56px; border-radius:12px; display:flex; align-items:center; justify-content:center; cursor:pointer; border:2px solid transparent; transition:all 0.2s; }
.theme-item.selected { border-color:#ff4d79; }
.theme-emoji { font-size:24px; }
.student-list { margin-top:12px; display:flex; flex-direction:column; gap:8px; }
.board-list { display:flex; flex-direction:column; gap:8px; }
.board-item { display:flex; align-items:center; gap:12px; padding:10px 12px; background:#fafafa; border-radius:10px; }
.board-info { flex:1; display:flex; align-items:center; gap:8px; }
.board-type { background:#fef3c7; color:#92400e; font-size:11px; padding:2px 8px; border-radius:6px; }
.board-name { font-weight:500; color:#333; }
.board-status { font-size:12px; }
.board-status.on { color:#16a34a; }
.board-status.off { color:#a8a29e; }
.switch { position:relative; display:inline-block; width:44px; height:24px; flex-shrink:0; }
.switch input { opacity:0; width:0; height:0; }
.slider { position:absolute; cursor:pointer; inset:0; background:#ccc; transition:0.3s; border-radius:24px; }
.slider::before { position:absolute; content:""; height:18px; width:18px; left:3px; bottom:3px; background:white; transition:0.3s; border-radius:50%; }
.switch input:checked + .slider { background:#ff6b9d; }
.switch input:checked + .slider::before { transform:translateX(20px); }
.list-toolbar { display:flex; align-items:center; justify-content:space-between; gap:8px; padding:6px 0; }
.checkbox-label { display:flex; align-items:center; gap:6px; font-size:13px; color:#4a5568; cursor:pointer; }
.student-item { background:#f7f3f0; border-radius:8px; padding:8px 12px; display:flex; align-items:center; gap:8px; font-size:14px; }
.student-checkbox { cursor:pointer; }
.student-name { flex:1; }
.student-no { color:#718096; font-size:12px; }
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