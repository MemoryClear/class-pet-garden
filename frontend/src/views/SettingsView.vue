<template>
  <AppShell title="系统设置" subtitle="系统名称、学生登录入口、外观与主题">
    <div class="container">
      <!-- 系统信息 -->
      <div class="section-card">
        <div class="section-title">⚙️ 系统信息</div>
        <div class="form-group">
          <label class="form-label">系统名称</label>
          <input type="text" class="form-input" v-model="form.systemName" @change="save" placeholder="如：课堂宠物乐园">
        </div>
        <div class="current-class">
          <div class="cc-main">
            <span class="cc-label">当前班级</span>
            <b>{{ currentClass?.name || '—' }}</b>
            <span class="cc-meta">
              班级号 <em>{{ currentClass?.code || '—' }}</em>
              · {{ currentClass?.studentCount ?? 0 }} 人
              · 学号 {{ currentClass?.code || 'C7F3' }}-S0001
            </span>
          </div>
          <button class="btn-ghost sm" @click="router.push('/classes')">管理班级 →</button>
        </div>
        <div class="form-hint">班级名称、公开看板、新增与删除都在「班级管理」页面里。</div>
      </div>

      <!-- 学生登录入口 -->
      <div class="section-card" id="student-access">
        <div class="section-title">🎒 学生登录入口</div>

        <div class="access-code">
          <div class="ac-main">
            <span class="ac-label">教师码</span>
            <b class="ac-code">{{ teacherCode || '—' }}</b>
          </div>
          <button class="btn-ghost sm" :disabled="!teacherCode" @click="copy(teacherCode, '教师码')">复制</button>
        </div>

        <ol class="access-steps">
          <li>学生打开登录页，切到「学生」标签</li>
          <li>输入教师码 <b>{{ teacherCode || '（同登录用户名）' }}</b>，选择自己的班级</li>
          <li>学号只填 <b>S0001</b> 这一段，班级号由下拉框决定，不用手输</li>
        </ol>

        <div class="access-note">
          💡 每个班级的登录说明，可以到「班级管理」页面点卡片上的 📋 图标复制。
        </div>

        <div class="form-hint">
          教师码就是你的登录用户名，学生只靠它找到你名下的班级。
          它是账号的一部分，不支持在这里修改。
        </div>
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

      <!-- 管理入口 -->
      <div class="section-card">
        <div class="section-title">🧩 管理入口</div>
        <p class="section-desc">
          学生、班级、评分项都已独立成页：学生管理维护名单与密码；班级管理负责新建 / 改名 / 看板 / 切换；评分项管理维护加减分模板。
        </p>
        <div class="entry-row">
          <button class="btn-sm" @click="router.push('/students')">👥 学生管理 →</button>
          <button class="btn-ghost" @click="router.push('/classes')">🏫 班级管理 →</button>
          <button class="btn-ghost" @click="router.push('/score-items')">🏷️ 评分项 →</button>
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
import { reactive, computed, watch, onMounted, nextTick } from 'vue'
import { useAppStore } from '../stores/app.js'
import { useAuthStore } from '../stores/auth.js'
import { useRouter, useRoute } from 'vue-router'
import $confirm from '../composables/useConfirmModal.js'
import AppShell from '../components/layout/AppShell.vue'
import { copyText } from '../utils/clipboard.js'
import { toast } from '../composables/useToast.js'


const appStore = useAppStore()
const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({ systemName: '', theme: 'pink', themeMode: 'light' })
const currentClass = computed(() => appStore.currentClass)

// 教师码：后端 /api/settings 返回，兜底用登录信息里的用户名
const teacherCode = computed(
  () => appStore.settings.teacherCode || authStore.user?.username || ''
)

async function copy(value, label) {
  if (!value) return
  const ok = await copyText(value)
  if (ok) toast(`${label}已复制`)
  else $confirm.error('复制失败，请手动选中复制')
}
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

async function selectMode(m) {
  if (form.themeMode === m) return
  form.themeMode = m
  await save()
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
  await appStore.fetchSettings()
  Object.assign(form, {
    systemName: appStore.settings.systemName || '',
    theme: appStore.settings.theme || 'pink',
    themeMode: appStore.settings.themeMode === 'dark' ? 'dark' : 'light'
  })
  await appStore.fetchClasses()

  scrollToAccess()
})

// 从侧栏「学生登录入口」跳过来时，滚到那张卡（同页重复点击也能生效）
function scrollToAccess() {
  if (route.hash !== '#student-access') return
  nextTick(() => {
    document.getElementById('student-access')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

watch(() => route.hash, scrollToAccess)
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

/* 学生登录入口 */
.access-code {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--r-sm);
  background: var(--brand-soft);
  border: 1px dashed var(--brand-soft-2);
  margin-bottom: 12px;
}
.ac-main { display: flex; align-items: baseline; gap: 10px; flex: 1; min-width: 0; }
.ac-label { font-size: 11.5px; color: var(--ink-3); flex: 0 0 auto; }
.ac-code {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: .04em;
  color: var(--brand-deep);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  word-break: break-all;
}
.access-steps {
  margin: 0 0 12px;
  padding-left: 18px;
  font-size: 12.5px;
  line-height: 1.95;
  color: var(--ink-2);
}
.access-steps b { color: var(--ink); font-weight: 600; }
.access-note {
  padding: 9px 12px;
  border-radius: var(--r-sm);
  background: var(--surface-2, #FAF7F6);
  border: 1px solid var(--line);
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-3);
}
.btn-ghost.sm { padding: 6px 12px; font-size: 12px; flex: 0 0 auto; }

@media (max-width: 768px) {
  .access-steps { font-size: 12px; line-height: 1.85; }
  .ac-code { font-size: 17px; }
  .class-row { flex-wrap: wrap; row-gap: 6px; }
  .class-row-main { flex: 1 1 100%; }
  .class-row-actions { margin-left: auto; }
}

/* 当前班级摘要 */
.current-class {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 14px;
  margin-bottom: 8px;
  border-radius: var(--r-sm);
  background: var(--brand-soft);
  border: 1px dashed var(--brand-soft-2);
}
.cc-main { flex: 1; min-width: 0; }
.cc-label { display: block; font-size: 10.5px; color: var(--ink-3); margin-bottom: 2px; }
.cc-main b { font-size: 14px; font-weight: 700; color: var(--ink); }
.cc-meta { display: block; font-size: 11.5px; color: var(--ink-3); margin-top: 3px; }
.cc-meta em { font-style: normal; font-weight: 600; color: var(--brand-deep); }

/* 入口按钮组 */
.entry-row { display: flex; gap: 8px; flex-wrap: wrap; }
.entry-row .btn-ghost { padding: 10px 16px; font-size: 14px; }

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
.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  transition: background .14s;
  touch-action: manipulation;
}
.icon-btn:hover { background: var(--line-soft); }
.error-msg { color:#ff4444; font-size:12px; display:none; }
.error-msg.show { display:block; }
.btn-logout { width:100%; padding:12px; background:#fff; border:1px solid #ff4444; border-radius:8px; color:#ff4444; cursor:pointer; font-size:14px; }
</style>