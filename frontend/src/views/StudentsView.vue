<template>
  <AppShell title="学生管理" :subtitle="subtitle">
    <template #search>
      <div class="search">
        <span class="s-ico">🔍</span>
        <input v-model="keyword" type="text" placeholder="搜索姓名 / 学号">
      </div>
    </template>

    <template #actions>
      <button class="btn-ghost batch-entry" @click="openAdd('batch')">📥 批量导入</button>
      <button class="btn-primary add-entry" @click="openAdd('single')">＋<span class="add-label"> 添加学生</span></button>
    </template>

    <div class="page-wrap">
      <!-- 概览 -->
      <section class="stats">
        <div class="stat accent">
          <div class="k">👥 学生总数</div>
          <div class="v">{{ stats.total }}<i>人</i></div>
          <div class="d">{{ appStore.settings.className || '当前班级' }}</div>
        </div>
        <div class="stat">
          <div class="k">🐾 已领养</div>
          <div class="v">{{ stats.adopted }}<i>人</i></div>
          <div class="d">还没领养 {{ stats.total - stats.adopted }} 人</div>
        </div>
        <div class="stat">
          <div class="k">🍖 班级口粮</div>
          <div class="v">{{ stats.food }}</div>
          <div class="d">全班累计</div>
        </div>
        <div class="stat">
          <div class="k">⭐ 平均等级</div>
          <div class="v">{{ stats.avgLevel }}</div>
          <div class="d">满级 6 级</div>
        </div>
      </section>

      <!-- 工具条 -->
      <section class="toolbar">
        <button
          v-for="f in filters"
          :key="f.key"
          class="chip"
          :class="{ on: filter === f.key }"
          @click="filter = f.key"
        >{{ f.label }}<b>{{ f.count }}</b></button>
        <div class="tool-right">
          <button class="btn-ghost" :disabled="assigning" @click="assignPets">
            {{ assigning ? '分配中…' : '🎲 一键随机领养' }}
          </button>
        </div>
      </section>

      <!-- 空状态 -->
      <div v-if="appStore.students.length === 0" class="empty-state">
        <div class="empty-icon">👥</div>
        <div class="empty-text">还没有学生</div>
        <div class="empty-hint">可以逐个添加，也可以把名单一次性粘进来</div>
        <button class="btn-primary" @click="openAdd('single')">添加第一个学生</button>
      </div>

      <div v-else-if="filtered.length === 0" class="empty-state">
        <div class="empty-icon">🔍</div>
        <div class="empty-text">没有找到匹配的学生</div>
        <div class="empty-hint">试试换个关键词，或切到「全部」</div>
      </div>

      <!-- 名单 -->
      <div v-else class="table">
        <div class="thead">
          <label class="c-check">
            <input type="checkbox" :checked="allChecked" @change="toggleAll">
          </label>
          <span class="c-stu">学生</span>
          <span class="c-no">学号</span>
          <span class="c-pet">伙伴</span>
          <span class="c-level">等级</span>
          <span class="c-food">口粮</span>
          <span class="c-act">操作</span>
        </div>

        <div
          v-for="stu in filtered"
          :key="stu.id"
          class="trow"
          :class="{ picked: selectedIds.includes(stu.id) }"
        >
          <label class="c-check">
            <input type="checkbox" :value="stu.id" v-model="selectedIds">
          </label>

          <div class="c-stu">
            <div class="mini-avatar" :class="{ empty: !stu.petIcon }">
              <PetIcon v-if="stu.petIcon" :icon="stu.petIcon" size="24" />
              <span v-else>🥚</span>
            </div>
            <span class="stu-name">{{ stu.name }}</span>
          </div>

          <span class="c-no mono">{{ stu.studentNo || '—' }}</span>

          <span class="c-pet">{{ stu.petName || '未领养' }}</span>

          <span class="c-level">
            <b class="lv-badge" :class="`lv-${levelOf(stu.food)}`">Lv.{{ levelOf(stu.food) }}</b>
            <span class="lv-name">{{ levelInfo(levelOf(stu.food)).name }}</span>
          </span>

          <span class="c-food">🍖 {{ stu.food ?? 0 }}</span>

          <div class="c-act">
            <button class="row-btn" title="编辑姓名 / 学号" @click="editStudent = stu">✏️</button>
            <button class="row-btn" title="重置密码" @click="resetPwd(stu)">🔑</button>
            <button class="row-btn danger" title="删除学生" @click="removeStudent(stu)">🗑️</button>
          </div>
        </div>
      </div>

      <!-- 批量操作条 -->
      <transition name="slide-up">
        <div v-if="selectedIds.length > 0" class="bulk-bar">
          <span class="bulk-text">已选择 <b>{{ selectedIds.length }}</b> 位学生</span>
          <button class="btn-ghost" @click="batchReset">🔑 批量重置密码</button>
          <button class="btn-ghost" @click="selectedIds = []">取消选择</button>
        </div>
      </transition>
    </div>

    <!-- 添加学生弹窗 -->
    <div v-if="showAdd" class="modal-overlay" @click.self="closeAdd">
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title">添加学生</div>
          <button class="modal-close" @click="closeAdd">×</button>
        </div>

        <div class="seg">
          <button :class="{ on: addMode === 'single' }" @click="addMode = 'single'">单个添加</button>
          <button :class="{ on: addMode === 'batch' }" @click="addMode = 'batch'">批量导入</button>
        </div>

        <div class="modal-body">
          <template v-if="addMode === 'single'">
            <div class="field">
              <label>姓名</label>
              <input v-model="form.name" class="input" maxlength="10" placeholder="如：林小满" @keyup.enter="submitAdd">
            </div>
            <div class="field">
              <label>初始密码（可选）</label>
              <input v-model="form.password" class="input" placeholder="留空则使用学号" @keyup.enter="submitAdd">
            </div>
            <div class="hint">留空时默认密码 = 学号；学生首次登录会被要求修改密码。</div>
          </template>

          <template v-else>
            <div class="field">
              <label>学生名单（每行一个姓名）</label>
              <textarea
                v-model="form.names"
                class="input textarea"
                rows="7"
                placeholder="张三&#10;李四&#10;王五"
              ></textarea>
            </div>
            <div class="field">
              <label>统一初始密码（可选）</label>
              <input v-model="form.password" class="input" placeholder="留空则各自使用学号">
            </div>
            <div class="hint">支持直接粘 Excel 的一列姓名；空行与重复姓名会自动跳过。</div>
          </template>

          <p v-if="addError" class="error-msg">{{ addError }}</p>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="closeAdd">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="submitAdd">
            {{ submitting ? '提交中…' : (addMode === 'single' ? '添加' : '导入') }}
          </button>
        </div>
      </div>
    </div>

    <StudentEditModal
      v-if="editStudent"
      :student="editStudent"
      @close="editStudent = null"
      @updated="onUpdated"
    />
  </AppShell>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'
import AppShell from '../components/layout/AppShell.vue'
import StudentEditModal from '../components/StudentEditModal.vue'
import PetIcon from '../components/PetIcon.vue'
import api from '../api/index.js'
import $confirm from '../composables/useConfirmModal.js'
import { levelOf, levelInfo, MAX_LEVEL } from '../constants/petLevels.js'

const appStore = useAppStore()

const keyword = ref('')
const filter = ref('all')
const selectedIds = ref([])
const assigning = ref(false)

const showAdd = ref(false)
const addMode = ref('single')
const addError = ref('')
const submitting = ref(false)
const form = reactive({ name: '', names: '', password: '' })
const editStudent = ref(null)

const subtitle = computed(() => {
  const n = appStore.students.length
  if (!n) return '先把班级名单建起来'
  return `${n} 位同学 · 学号用于学生端登录`
})

const stats = computed(() => {
  const list = appStore.students
  const total = list.length
  const adopted = list.filter(s => s.petId).length
  const food = list.reduce((sum, s) => sum + (s.food ?? 0), 0)
  const levelSum = list.reduce((sum, s) => sum + levelOf(s.food), 0)
  return {
    total,
    adopted,
    food: food.toLocaleString('zh-CN'),
    avgLevel: total ? (levelSum / total).toFixed(1) : '0.0'
  }
})

const filters = computed(() => {
  const list = appStore.students
  return [
    { key: 'all', label: '全部', count: list.length },
    { key: 'adopted', label: '已领养', count: list.filter(s => s.petId).length },
    { key: 'unadopted', label: '待领养', count: list.filter(s => !s.petId).length },
    { key: 'max', label: '满级', count: list.filter(s => levelOf(s.food) >= MAX_LEVEL).length }
  ]
})

const filtered = computed(() => {
  let list = appStore.students
  if (filter.value === 'adopted') list = list.filter(s => s.petId)
  else if (filter.value === 'unadopted') list = list.filter(s => !s.petId)
  else if (filter.value === 'max') list = list.filter(s => levelOf(s.food) >= MAX_LEVEL)

  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(s =>
      (s.name || '').toLowerCase().includes(kw) ||
      (s.studentNo || '').toLowerCase().includes(kw)
    )
  }
  return [...list].sort((a, b) => (a.studentNo || '').localeCompare(b.studentNo || ''))
})

const allChecked = computed(() =>
  filtered.value.length > 0 && filtered.value.every(s => selectedIds.value.includes(s.id))
)

function toggleAll(e) {
  if (e.target.checked) {
    const ids = new Set(selectedIds.value)
    filtered.value.forEach(s => ids.add(s.id))
    selectedIds.value = [...ids]
  } else {
    const visible = new Set(filtered.value.map(s => s.id))
    selectedIds.value = selectedIds.value.filter(id => !visible.has(id))
  }
}

function openAdd(mode) {
  addMode.value = mode || 'single'
  addError.value = ''
  showAdd.value = true
}

function closeAdd() {
  showAdd.value = false
  form.name = ''
  form.names = ''
  form.password = ''
  addError.value = ''
}

async function submitAdd() {
  addError.value = ''
  const pwd = form.password.trim()
  submitting.value = true
  try {
    if (addMode.value === 'single') {
      if (!form.name.trim()) { addError.value = '请填写学生姓名'; submitting.value = false; return }
      const payload = { name: form.name.trim(), classId: appStore.currentClassId }
      if (pwd) payload.initialPassword = pwd
      await api.post('/students', payload)
      await appStore.fetchStudents()
      form.name = ''
      form.password = ''
      showAdd.value = false
    } else {
      if (!form.names.trim()) { addError.value = '请粘贴学生名单'; submitting.value = false; return }
      const payload = { names: form.names, classId: appStore.currentClassId }
      if (pwd) payload.initialPassword = pwd
      await api.post('/students/batch', payload)
      await appStore.fetchStudents()
      const count = appStore.students.length
      closeAdd()
      $confirm.success(`导入完成，当前班级共 ${count} 位学生`)
    }
  } catch (e) {
    addError.value = e.response?.data?.error || e.message || '添加失败'
  } finally {
    submitting.value = false
  }
}

async function resetPwd(stu) {
  const pwd = await $confirm.prompt(`为「${stu.name}」设置新密码：`, {
    title: '重置密码',
    inputType: 'text',
    inputPlaceholder: '留空则重置为学号',
    inputHint: `学号：${stu.studentNo || '—'}（留空将使用学号作为新密码）`,
    okText: '重置'
  })
  if (pwd === null) return
  try {
    await api.post(`/students/${stu.id}/reset-password`, { newPassword: pwd.trim() || null })
    const actual = pwd.trim() || stu.studentNo
    $confirm.success(`已将「${stu.name}」的密码重置为：${actual}`)
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
  }
}

async function batchReset() {
  if (selectedIds.value.length === 0) return
  const pwd = await $confirm.prompt(`即将批量重置 ${selectedIds.value.length} 个学生的密码：`, {
    title: '批量重置密码',
    inputType: 'text',
    inputPlaceholder: '留空则全部重置为各自学号',
    okText: '开始重置'
  })
  if (pwd === null) return
  try {
    const { data } = await api.post('/students/batch-reset-password', {
      studentIds: selectedIds.value,
      newPassword: pwd.trim() || null
    })
    $confirm.success(`批量重置完成：成功 ${data.success}，失败 ${data.failed}`)
    selectedIds.value = []
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
  }
}

async function removeStudent(stu) {
  const ok = await $confirm.confirm(
    `确定删除「${stu.name}」吗？该学生的宠物、口粮与积分记录都会一起移除，且无法恢复。`,
    { title: '删除学生', okText: '删除', icon: '🗑️', danger: true }
  )
  if (!ok) return
  try {
    await appStore.deleteStudent(stu.id)
    selectedIds.value = selectedIds.value.filter(id => id !== stu.id)
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
  }
}

async function assignPets() {
  const pending = appStore.students.filter(s => !s.petId).length
  if (!pending) {
    $confirm.alert('所有学生都已经有伙伴了 🎉')
    return
  }
  const ok = await $confirm.confirm(
    `将为 ${pending} 位还没领养的学生随机分配宠物，已领养的不受影响。`,
    { title: '一键随机领养', okText: '开始分配', icon: '🎲' }
  )
  if (!ok) return
  assigning.value = true
  try {
    await appStore.assignPetsRandomly()
    $confirm.success('分配完成')
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
  } finally {
    assigning.value = false
  }
}

function onUpdated() {
  editStudent.value = null
  appStore.fetchStudents()
}

onMounted(async () => {
  await appStore.fetchStudents()
  if (!appStore.settings.className) await appStore.fetchSettings()
})
</script>

<style scoped>
.page-wrap { max-width: 1040px; margin: 0 auto; padding-bottom: 40px; }

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
.stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
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

/* 工具条 */
.toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; flex-wrap: wrap; }
.chip {
  padding: 7px 13px;
  border-radius: 9px;
  background: var(--surface);
  border: 1px solid var(--line);
  font-size: 12.5px;
  color: var(--ink-2);
  cursor: pointer;
  font-weight: 500;
  transition: .16s;
  font-family: inherit;
}
.chip b { margin-left: 4px; font-weight: 600; opacity: .55; }
.chip.on { background: var(--pill-active-bg); border-color: var(--pill-active-bg); color: var(--pill-active-fg); font-weight: 600; }
.chip.on b { opacity: .75; }
.tool-right { margin-left: auto; }

/* 名单 */
.table {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-s);
  overflow: hidden;
}
.thead,
.trow {
  display: grid;
  grid-template-columns: 40px minmax(140px, 1.4fr) 120px 96px 130px 84px 108px;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
}
.thead {
  font-size: 11.5px;
  color: var(--ink-3);
  background: var(--line-soft);
  border-bottom: 1px solid var(--line);
  font-weight: 600;
}
.trow { border-bottom: 1px solid var(--line-soft); transition: background .14s; }
.trow:last-child { border-bottom: none; }
.trow:hover { background: #FCFAF9; }
.trow.picked { background: var(--brand-soft); }

input[type="checkbox"] { width: 15px; height: 15px; cursor: pointer; accent-color: var(--brand); }

.c-stu { display: flex; align-items: center; gap: 9px; min-width: 0; }
.mini-avatar {
  width: 34px; height: 34px;
  flex: 0 0 34px;
  border-radius: 50%;
  background: var(--line-soft);
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  font-size: 16px;
}
.mini-avatar.empty { opacity: .7; }
.stu-name { font-size: 13.5px; font-weight: 600; color: var(--ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.c-no { font-size: 12.5px; color: var(--ink-3); }
.mono { font-variant-numeric: tabular-nums; letter-spacing: .01em; }
.c-pet { font-size: 12.5px; color: var(--ink-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.c-level { display: flex; align-items: center; gap: 6px; }
.lv-badge {
  font-size: 10.5px;
  font-weight: 700;
  color: #fff;
  padding: 1px 7px;
  border-radius: 6px;
  background: #A9AFBB;
}
.lv-badge.lv-2 { background: #60A5FA; }
.lv-badge.lv-3 { background: #A78BFA; }
.lv-badge.lv-4 { background: #3B82F6; }
.lv-badge.lv-5 { background: #F97316; }
.lv-badge.lv-6 { background: linear-gradient(135deg, #FFC94D, #F09000); }
.lv-name { font-size: 11.5px; color: var(--ink-3); }
.c-food { font-size: 12.5px; font-weight: 600; color: #B87323; font-variant-numeric: tabular-nums; }
.c-act { display: flex; gap: 4px; justify-content: flex-end; }
.row-btn {
  width: 28px; height: 28px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: var(--surface);
  cursor: pointer;
  font-size: 12px;
  display: flex; align-items: center; justify-content: center;
  transition: .16s;
  touch-action: manipulation;
}
.row-btn:hover { background: var(--line-soft); }
.row-btn.danger:hover { background: #FEF2F2; border-color: #FCA5A5; }

/* 批量条 */
.bulk-bar {
  position: sticky;
  bottom: 16px;
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-m);
}
.bulk-text { font-size: 13px; color: var(--ink-2); }
.bulk-text b { color: var(--ink); }
.slide-up-enter-active, .slide-up-leave-active { transition: .2s ease; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(10px); }

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 56px 20px;
  background: var(--surface);
  border: 1px dashed var(--line);
  border-radius: var(--r-lg);
}
.empty-icon {
  width: 72px; height: 72px;
  margin: 0 auto 14px;
  border-radius: 50%;
  background: var(--line-soft);
  display: flex; align-items: center; justify-content: center;
  font-size: 30px;
}
.empty-text { font-size: 15px; font-weight: 600; color: var(--ink); margin-bottom: 6px; }
.empty-hint { font-size: 12.5px; color: var(--ink-3); margin-bottom: 18px; }

/* 添加弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(20, 22, 30, .45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}
.modal-card {
  background: var(--surface);
  border-radius: var(--r-lg);
  width: 100%;
  max-width: 440px;
  box-shadow: var(--shadow-m);
  overflow: hidden;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--line);
}
.modal-title { font-size: 15.5px; font-weight: 700; color: var(--ink); }
.modal-close {
  width: 30px; height: 30px;
  border: 0;
  border-radius: 50%;
  background: var(--line-soft);
  color: var(--ink-2);
  font-size: 16px;
  cursor: pointer;
}
.seg { display: flex; gap: 4px; margin: 16px 20px 0; background: #F4EFEE; padding: 4px; border-radius: 11px; }
.seg button {
  flex: 1;
  border: 0;
  background: transparent;
  padding: 8px 0;
  border-radius: 8px;
  font-size: 12.5px;
  font-family: inherit;
  color: #767D8C;
  cursor: pointer;
  transition: .16s;
}
.seg button.on { background: var(--surface); color: var(--ink); font-weight: 600; box-shadow: 0 1px 3px rgba(0, 0, 0, .07); }

.modal-body { padding: 16px 20px 6px; }
.field { margin-bottom: 12px; }
.field label { display: block; font-size: 12.5px; font-weight: 600; color: var(--ink-2); margin-bottom: 6px; }
.input {
  width: 100%;
  height: 42px;
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  background: var(--surface);
  padding: 0 12px;
  font-size: 13.5px;
  font-family: inherit;
  color: var(--ink);
  transition: .16s;
}
.input:focus { outline: 0; border-color: var(--brand); box-shadow: 0 0 0 3px var(--brand-soft); }
.textarea { height: auto; padding: 10px 12px; line-height: 1.7; resize: vertical; }
.hint { font-size: 11.5px; color: var(--ink-3); line-height: 1.6; }
.error-msg {
  font-size: 12.5px;
  color: #C0392B;
  background: #FDF1EF;
  border: 1px solid #F8DBD6;
  border-radius: 9px;
  padding: 8px 11px;
  margin-top: 8px;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px 18px;
}
.btn-cancel {
  padding: 9px 18px;
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  background: var(--surface);
  font-size: 13.5px;
  color: var(--ink-2);
  cursor: pointer;
  font-family: inherit;
}

@media (max-width: 980px) {
  .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .thead, .trow { grid-template-columns: 34px minmax(120px, 1.4fr) 100px 110px 78px; }
  .c-no, .c-pet { display: none; }
}

@media (max-width: 768px) {
  .search { width: 100%; }
  .search input { width: 100%; height: 34px; }
  .search input:focus { width: 100%; }
  .stats { gap: 8px; }
  .stat { padding: 10px 12px; }
  .stat .v { font-size: 19px; }
  .tool-right { margin-left: 0; width: 100%; }
  .batch-entry { display: none; }
  .add-label { display: none; }
  .add-entry { padding: 9px 12px; font-size: 15px; }
  .table { border: none; background: transparent; box-shadow: none; overflow: visible; }
  .thead { display: none; }
  .trow {
    grid-template-columns: 28px minmax(0, 1fr) auto;
    gap: 8px;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: var(--r-md);
    padding: 11px 12px;
    margin-bottom: 8px;
  }
  .trow:last-child { border-bottom: 1px solid var(--line); }
  .c-no, .c-pet, .c-level, .c-food { display: none; }
  .c-act { gap: 6px; }
  .row-btn { width: 32px; height: 32px; }
  .bulk-bar { bottom: 76px; }
}
</style>
