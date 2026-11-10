<template>
  <AppShell title="班级管理" :subtitle="subtitle">
    <template #search>
      <div class="search">
        <span class="s-ico">🔍</span>
        <input v-model="keyword" type="text" placeholder="搜索班级">
      </div>
    </template>

    <template #actions>
      <button class="btn-primary create-btn" @click="openCreate">＋<span class="create-label"> 新建班级</span></button>
    </template>

    <div class="page-wrap">
      <!-- 概览 -->
      <section class="stats">
        <div class="stat accent">
          <div class="k">🏫 班级总数</div>
          <div class="v">{{ stats.classCount }}<i>个</i></div>
          <div class="d">当前使用「{{ appStore.className }}」</div>
        </div>
        <div class="stat">
          <div class="k">👥 学生总数</div>
          <div class="v">{{ stats.studentTotal }}<i>人</i></div>
          <div class="d">所有班级合计</div>
        </div>
        <div class="stat">
          <div class="k">📺 公开看板</div>
          <div class="v">{{ stats.boardOn }}<i>个</i></div>
          <div class="d">无需登录即可查看</div>
        </div>
        <div class="stat">
          <div class="k">📊 平均班额</div>
          <div class="v">{{ stats.avgSize }}<i>人</i></div>
          <div class="d">每个班级的平均人数</div>
        </div>
      </section>

      <!-- 说明 -->
      <div class="tipbar">
        <span class="tip-ico">💡</span>
        <span>
          学生、加减分历史、兑换记录、光荣榜都按班级隔离；切换班级后左侧所有页面只显示该班数据。
          评分项与小卖部商品是全教师共享的。
        </span>
      </div>

      <!-- 空状态 -->
      <div v-if="appStore.classes.length === 0" class="empty-state">
        <div class="empty-icon">🏫</div>
        <div class="empty-text">还没有班级</div>
        <div class="empty-hint">先建一个班级，学生才能归属进来</div>
        <button class="btn-primary" @click="openCreate">新建第一个班级</button>
      </div>

      <div v-else-if="filtered.length === 0" class="empty-state">
        <div class="empty-icon">🔍</div>
        <div class="empty-text">没有找到匹配的班级</div>
        <div class="empty-hint">试试班级名或班级号，如 C7F3</div>
      </div>

      <!-- 班级卡片 -->
      <section v-else class="grid">
        <article
          v-for="c in filtered"
          :key="c.id"
          class="ccard"
          :class="{ current: isCurrent(c) }"
        >
          <header class="cc-head">
            <span class="cc-ico">🏫</span>
            <div class="cc-title">
              <b :title="c.name">{{ c.name }}</b>
              <span class="cc-sub">
                班级号 <em class="mono">{{ c.code }}</em>
              </span>
            </div>
            <span v-if="isCurrent(c)" class="cur-tag">当前</span>
          </header>

          <div class="cc-meta">
            <div>
              <span>学生人数</span>
              <b>{{ c.studentCount ?? 0 }} 人</b>
            </div>
            <div>
              <span>学号格式</span>
              <b class="mono">{{ c.code }}-S0001</b>
            </div>
          </div>

          <label class="cc-board">
            <span class="cb-text">
              公开看板
              <i>开启后无需登录也能看这个班的榜单</i>
            </span>
            <span class="switch">
              <input
                type="checkbox"
                :checked="!!c.showOnBoard"
                :disabled="!!boardBusy[c.id]"
                @change="toggleBoard(c, $event)"
              >
              <span class="slider"></span>
            </span>
          </label>

          <footer class="cc-actions">
            <button
              class="btn-ghost sm"
              :class="{ on: isCurrent(c) }"
              :disabled="isCurrent(c)"
              @click="switchTo(c)"
            >{{ isCurrent(c) ? '✓ 正在使用' : '切换到此班' }}</button>
            <button class="row-btn" title="重命名" @click="rename(c)">✏️</button>
            <button class="row-btn" title="复制学生登录说明" @click="copyInvite(c)">📋</button>
            <button class="row-btn danger" title="删除班级" @click="remove(c)">🗑️</button>
          </footer>
        </article>
      </section>
    </div>

    <!-- 新建班级 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="closeCreate">
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title">新建班级</div>
          <button class="modal-close" @click="closeCreate">×</button>
        </div>

        <div class="modal-body">
          <div class="field">
            <label>班级名称</label>
            <input
              ref="nameInput"
              v-model="form.name"
              class="input"
              maxlength="20"
              placeholder="如：初二(1)班"
              @keyup.enter="submitCreate"
            >
          </div>
          <div class="hint">
            班级号（如 <span class="mono">C7F3</span>）由系统自动生成，学生学号就是「班级号-S0001」。
            创建后会自动切换到新班级。
          </div>
          <p v-if="createError" class="error-msg">{{ createError }}</p>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="closeCreate">取消</button>
          <button class="btn-primary" :disabled="creating" @click="submitCreate">
            {{ creating ? '创建中…' : '创建' }}
          </button>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useAppStore } from '../stores/app.js'
import { useAuthStore } from '../stores/auth.js'
import AppShell from '../components/layout/AppShell.vue'
import { classApi } from '../api/index.js'
import { copyText } from '../utils/clipboard.js'
import { buildClassInvite } from '../utils/invite.js'
import { toast } from '../composables/useToast.js'
import $confirm from '../composables/useConfirmModal.js'

const appStore = useAppStore()
const authStore = useAuthStore()

const keyword = ref('')
const showCreate = ref(false)
const creating = ref(false)
const createError = ref('')
const nameInput = ref(null)
const boardBusy = ref({})
const form = reactive({ name: '' })

// 从学生管理切过来时，学生数可能刚变过，进页面重新拉一次班级列表
onMounted(() => { appStore.fetchClasses().catch(() => {}) })

const subtitle = computed(() => {
  const n = appStore.classes.length
  if (!n) return '先建一个班级，学生才能归属进来'
  return `${n} 个班级 · 切换后左侧菜单只显示该班数据`
})

const stats = computed(() => {
  const list = appStore.classes
  const classCount = list.length
  const studentTotal = list.reduce((sum, c) => sum + (c.studentCount ?? 0), 0)
  const boardOn = list.filter(c => c.showOnBoard).length
  return {
    classCount,
    studentTotal,
    boardOn,
    avgSize: classCount ? (studentTotal / classCount).toFixed(1) : '0.0'
  }
})

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const list = kw
    ? appStore.classes.filter(c =>
        (c.name || '').toLowerCase().includes(kw) ||
        (c.code || '').toLowerCase().includes(kw))
    : appStore.classes
  return [...list].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
})

function isCurrent(c) {
  return c.id === appStore.currentClassId
}

function openCreate() {
  form.name = ''
  createError.value = ''
  showCreate.value = true
  nextTick(() => nameInput.value?.focus())
}

function closeCreate() {
  showCreate.value = false
  creating.value = false
  createError.value = ''
}

async function submitCreate() {
  const name = form.name.trim()
  if (!name) {
    createError.value = '请先给班级起个名字'
    nameInput.value?.focus()
    return
  }
  creating.value = true
  createError.value = ''
  try {
    const { data } = await classApi.create(name)
    await appStore.fetchClasses()
    await appStore.switchClass(data.id)
    toast(`已创建「${data.name}」，班级号 ${data.code}`)
    closeCreate()
  } catch (e) {
    createError.value = e.response?.data?.error || e.message
  } finally {
    creating.value = false
  }
}

async function switchTo(c) {
  if (isCurrent(c)) return
  try {
    await appStore.switchClass(c.id)
    toast(`已切换到「${c.name}」`)
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message || '切换失败')
  }
}

async function toggleBoard(c, e) {
  const next = !!e.target.checked
  boardBusy.value = { ...boardBusy.value, [c.id]: true }
  try {
    await classApi.setBoard(c.id, next)
    await appStore.fetchClasses()
    toast(next ? `「${c.name}」已加入公开看板` : `「${c.name}」已从公开看板移除`)
  } catch (err) {
    e.target.checked = !next
    $confirm.error(err.response?.data?.error || err.message || '设置失败')
  } finally {
    const next2 = { ...boardBusy.value }
    delete next2[c.id]
    boardBusy.value = next2
  }
}

async function rename(c) {
  const name = await $confirm.prompt(`修改「${c.name}」的名称：`, {
    title: '重命名班级',
    inputValue: c.name,
    inputRequired: true,
    okText: '保存'
  })
  if (name === null || !name.trim()) return
  try {
    await classApi.rename(c.id, name.trim())
    await appStore.fetchClasses()
    if (isCurrent(c)) await appStore.fetchSettings()
    toast('班级名称已更新')
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
  }
}

async function remove(c) {
  const count = c.studentCount ?? 0
  const ok = await $confirm.confirm(
    count > 0
      ? `「${c.name}」里还有 ${count} 位学生，需要先把学生转走或删除才能删班级。仍要尝试删除吗？`
      : `删除「${c.name}」后，班级号 ${c.code} 不再使用，且不可恢复。确定删除吗？`,
    { title: '删除班级', okText: '删除', icon: '🗑️', danger: true }
  )
  if (!ok) return
  try {
    await classApi.remove(c.id)
    await appStore.fetchClasses()
    await appStore.fetchStudents()
    toast(`已删除「${c.name}」`)
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
  }
}

async function copyInvite(c) {
  const ok = await copyText(buildClassInvite({
    systemName: appStore.settings.systemName,
    teacherCode: appStore.settings.teacherCode || authStore.user?.username,
    className: c.name,
    classCode: c.code
  }))
  if (ok) toast(`「${c.name}」的登录说明已复制`)
  else $confirm.error('复制失败，请手动选中复制')
}
</script>

<style scoped>
.page-wrap { max-width: 1080px; margin: 0 auto; padding-bottom: 40px; }

/* 搜索 */
.search { position: relative; }
.search input {
  width: 230px;
  height: 36px;
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  background: var(--surface);
  padding: 0 12px 0 34px;
  font-size: 13px;
  font-family: inherit;
  color: var(--ink);
  transition: .18s;
}
.search input::placeholder { color: #BCC1CB; }
.search input:focus { outline: 0; width: 280px; border-color: var(--brand); box-shadow: 0 0 0 3px var(--brand-soft); }
.search .s-ico { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); font-size: 12px; opacity: .45; }

/* 概览 */
.stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.stat {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  padding: 13px 15px;
  box-shadow: var(--shadow-s);
}
.stat .k { font-size: 11.5px; color: var(--ink-3); }
.stat .v { font-size: 23px; font-weight: 700; letter-spacing: -.02em; margin-top: 6px; font-variant-numeric: tabular-nums; }
.stat .v i { font-style: normal; font-size: 12px; font-weight: 600; color: var(--ink-3); margin-left: 3px; }
.stat .d { font-size: 11px; color: var(--ink-3); margin-top: 3px; }
.stat.accent {
  background: linear-gradient(140deg, var(--brand-soft), rgba(255, 255, 255, .4));
  border-color: var(--brand-soft-2);
}

/* 说明条 */
.tipbar {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  background: var(--surface);
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-3);
}
.tip-ico { flex: 0 0 auto; }

/* 卡片网格 */
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 14px; }
.ccard {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 15px 16px 12px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-s);
  transition: box-shadow .18s, border-color .18s;
}
.ccard:hover { box-shadow: var(--shadow-m); }
.ccard.current {
  border-color: var(--brand-soft-2);
  box-shadow: 0 0 0 3px var(--brand-soft), var(--shadow-s);
}

.cc-head { display: flex; align-items: center; gap: 10px; }
.cc-ico {
  width: 38px; height: 38px;
  flex: 0 0 38px;
  border-radius: 11px;
  background: var(--brand-soft);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px;
}
.cc-title { flex: 1; min-width: 0; }
.cc-title b {
  display: block;
  font-size: 14.5px;
  font-weight: 700;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cc-sub { font-size: 11.5px; color: var(--ink-3); }
.cc-sub em { font-style: normal; color: var(--brand-deep); font-weight: 600; }
.cur-tag {
  flex: 0 0 auto;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--brand-deep);
  background: var(--brand-soft-2);
  padding: 2px 8px;
  border-radius: 6px;
}

.cc-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.cc-meta > div {
  padding: 8px 10px;
  border-radius: 9px;
  background: var(--line-soft);
}
.cc-meta span { display: block; font-size: 10.5px; color: var(--ink-3); }
.cc-meta b { font-size: 12.5px; font-weight: 600; color: var(--ink); }
.mono { font-variant-numeric: tabular-nums; letter-spacing: .01em; }

.cc-board {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;
}
.cb-text { display: flex; flex-direction: column; gap: 2px; font-size: 12.5px; color: var(--ink-2); }
.cb-text i { font-style: normal; font-size: 10.5px; color: var(--ink-3); }

.cc-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px solid var(--line-soft);
}
.cc-actions .btn-ghost { flex: 1; justify-content: center; }
.btn-ghost.sm { padding: 7px 10px; font-size: 12px; }
.btn-ghost.on { color: var(--brand-deep); border-color: var(--brand-soft-2); background: var(--brand-soft); opacity: 1; }
.btn-ghost:disabled { cursor: default; }

.row-btn {
  width: 30px; height: 30px;
  flex: 0 0 30px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: var(--surface);
  cursor: pointer;
  font-size: 12.5px;
  display: flex; align-items: center; justify-content: center;
  transition: .16s;
  touch-action: manipulation;
}
.row-btn:hover { background: var(--line-soft); }
.row-btn.danger:hover { background: #FEF2F2; border-color: #FCA5A5; }

/* 开关 */
.switch { position: relative; display: inline-block; width: 44px; height: 24px; flex: 0 0 44px; }
.switch input { opacity: 0; width: 0; height: 0; }
.slider { position: absolute; cursor: pointer; inset: 0; background: #ccc; transition: .3s; border-radius: 24px; }
.slider::before { position: absolute; content: ""; height: 18px; width: 18px; left: 3px; bottom: 3px; background: #fff; transition: .3s; border-radius: 50%; }
.switch input:checked + .slider { background: var(--brand); }
.switch input:checked + .slider::before { transform: translateX(20px); }
.switch input:disabled + .slider { opacity: .6; cursor: not-allowed; }

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 56px 20px;
  background: var(--surface);
  border: 1px dashed var(--line);
  border-radius: var(--r-md);
}
.empty-icon { font-size: 40px; opacity: .5; }
.empty-text { font-size: 14px; font-weight: 600; color: var(--ink); }
.empty-hint { font-size: 12.5px; color: var(--ink-3); margin-bottom: 6px; }

/* 新建班级弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(20, 20, 25, .42);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.modal-card {
  width: 100%;
  max-width: 400px;
  background: var(--surface);
  border-radius: 18px;
  box-shadow: var(--shadow-m);
  overflow: hidden;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px 10px;
}
.modal-title { font-size: 15px; font-weight: 700; color: var(--ink); }
.modal-close {
  border: 0;
  background: transparent;
  font-size: 20px;
  line-height: 1;
  color: var(--ink-3);
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 6px;
}
.modal-close:hover { background: var(--line-soft); }
.modal-body { padding: 4px 18px 14px; }
.field { margin-bottom: 12px; }
.field label { display: block; font-size: 12.5px; color: var(--ink-2); margin-bottom: 6px; }
.input {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1.5px solid var(--line);
  border-radius: var(--r-sm);
  background: var(--surface);
  font-size: 13.5px;
  font-family: inherit;
  color: var(--ink);
  transition: .18s;
}
.input:focus { outline: 0; border-color: var(--brand); box-shadow: 0 0 0 3px var(--brand-soft); }
.hint { font-size: 11.5px; line-height: 1.7; color: var(--ink-3); }
.hint .mono { color: var(--brand-deep); font-weight: 600; }
.error-msg { font-size: 12px; color: #DC2626; margin-top: 8px; }
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 18px 16px;
  border-top: 1px solid var(--line-soft);
}
.btn-cancel {
  padding: 9px 16px;
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  background: var(--surface);
  color: var(--ink-2);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}
.btn-cancel:hover { background: var(--line-soft); }

/* 移动端 */
@media (max-width: 900px) {
  .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .stat .v { font-size: 20px; }
}
@media (max-width: 768px) {
  .create-label { display: none; }
  .create-btn { padding: 9px 12px; font-size: 15px; }
  .search { width: 100%; }
  .search input { width: 100%; }
  .search input:focus { width: 100%; }
  .grid { grid-template-columns: 1fr; }
  .ccard { padding: 14px 14px 10px; }
}
</style>
