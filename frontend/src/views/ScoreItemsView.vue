<template>
  <AppShell title="评分项管理" :subtitle="subtitle">
    <template #search>
      <div class="search">
        <span class="s-ico">🔍</span>
        <input v-model="keyword" type="text" placeholder="搜索评分项">
      </div>
    </template>

    <template #actions>
      <button class="btn-primary create-btn" @click="openCreate">
        ＋<span class="create-label"> 新建评分项</span>
      </button>
    </template>

    <div class="page-wrap">
      <!-- 概览 -->
      <section class="stats">
        <div class="stat accent">
          <div class="k">🏷️ 评分项</div>
          <div class="v">{{ stats.total }}<i>项</i></div>
          <div class="d">加减分弹窗按这个顺序列出</div>
        </div>
        <div class="stat">
          <div class="k">➕ 加分项</div>
          <div class="v">{{ stats.plus }}<i>项</i></div>
          <div class="d">答对、早读、作业优秀…</div>
        </div>
        <div class="stat">
          <div class="k">➖ 减分项</div>
          <div class="v">{{ stats.minus }}<i>项</i></div>
          <div class="d">迟到、玩手机…</div>
        </div>
      </section>

      <div class="tipbar">
        💡 评分项是教师级的，所有班级共用；点宠物卡片的「加减分」时按这里的顺序显示，弹窗里还能 ×2 / ×3。
        删除评分项不会影响已经记下的加减分历史。
      </div>

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
          <span class="order-hint">按创建顺序排列，新加的排在最后</span>
        </div>
      </section>

      <!-- 空状态 -->
      <div v-if="appStore.scoreItems.length === 0" class="empty-state">
        <div class="empty-icon">🏷️</div>
        <div class="empty-text">还没有评分项</div>
        <div class="empty-hint">先加几个常用的：早读打卡 +1、答对问题 +2、迟到 -1</div>
        <button class="btn-primary" @click="openCreate">添加第一个评分项</button>
      </div>

      <div v-else-if="filtered.length === 0" class="empty-state">
        <div class="empty-icon">🔍</div>
        <div class="empty-text">没有找到匹配的评分项</div>
        <div class="empty-hint">试试换个关键词，或切到「全部」</div>
      </div>

      <!-- 评分项卡片 -->
      <section v-else class="grid">
        <article
          v-for="item in filtered"
          :key="item.id"
          class="scard"
          :class="item.point > 0 ? 'plus' : 'minus'"
        >
          <div class="sc-ico">{{ item.icon }}</div>

          <div class="sc-main">
            <b :title="item.name">{{ item.name }}</b>
            <span>{{ item.point > 0 ? '加分项' : '减分项' }}</span>
          </div>

          <div class="sc-pt">{{ item.point > 0 ? '+' : '' }}{{ item.point }}</div>

          <div class="sc-actions">
            <button class="row-btn" title="编辑" @click="openEdit(item)">✏️</button>
            <button class="row-btn danger" title="删除" @click="remove(item)">🗑️</button>
          </div>
        </article>
      </section>
    </div>

    <!-- 新建 / 编辑 -->
    <div v-if="showForm" class="modal-overlay" @click.self="closeForm">
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title">{{ editing ? '编辑评分项' : '新建评分项' }}</div>
          <button class="modal-close" @click="closeForm">×</button>
        </div>

        <div class="modal-body">
          <!-- 实时预览 -->
          <div class="preview" :class="form.point >= 0 ? 'plus' : 'minus'">
            <div class="pv-ico">{{ form.icon || '🏷️' }}</div>
            <div class="pv-main">
              <b>{{ form.name || '评分项名称' }}</b>
              <span>加减分弹窗里长这样</span>
            </div>
            <div class="pv-pt">{{ pointPreview }}</div>
          </div>

          <div class="field">
            <label>图标</label>
            <div class="emoji-row">
              <input v-model="form.icon" class="input emoji-input" maxlength="2" placeholder="🏷️">
              <div class="emoji-grid">
                <button
                  v-for="e in emojis"
                  :key="e"
                  class="emoji-btn"
                  :class="{ on: form.icon === e }"
                  @click="form.icon = e"
                >{{ e }}</button>
              </div>
            </div>
          </div>

          <div class="field">
            <label>名称</label>
            <input
              v-model="form.name"
              class="input"
              maxlength="20"
              placeholder="如：早读打卡"
              @keyup.enter="submit"
            >
          </div>

          <div class="field">
            <label>分值</label>
            <div class="point-row">
              <button class="step-btn" @click="stepPoint(-1)">−</button>
              <input v-model.number="form.point" type="number" class="input point-input">
              <button class="step-btn" @click="stepPoint(1)">＋</button>
              <span class="point-hint">正数加分，负数减分</span>
            </div>
            <div class="pt-chips">
              <button
                v-for="p in pointPresets"
                :key="p"
                class="pt-chip"
                :class="{ on: form.point === p, neg: p < 0 }"
                @click="form.point = p"
              >{{ p > 0 ? '+' + p : p }}</button>
            </div>
          </div>

          <p v-if="formError" class="error-msg">{{ formError }}</p>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="closeForm">取消</button>
          <button class="btn-primary" :disabled="submitting" @click="submit">
            {{ submitting ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useAppStore } from '../stores/app.js'
import AppShell from '../components/layout/AppShell.vue'
import { toast } from '../composables/useToast.js'
import $confirm from '../composables/useConfirmModal.js'

const appStore = useAppStore()

const keyword = ref('')
const filter = ref('all')

const showForm = ref(false)
const editing = ref(null)
const submitting = ref(false)
const formError = ref('')
const form = reactive({ icon: '', name: '', point: 1 })

const emojis = [
  '📖', '💡', '📘', '🎤', '🌱', '⭐', '🏅', '✅',
  '⏰', '📱', '😴', '❌', '🗣️', '🧹', '🤝', '💪'
]
const pointPresets = [-3, -2, -1, 1, 2, 3]

const subtitle = computed(() => {
  const n = appStore.scoreItems.length
  if (!n) return '先加几个常用的加减分模板'
  return `${n} 个模板 · 所有班级共用，加减分时直接取用`
})

const stats = computed(() => {
  const list = appStore.scoreItems
  return {
    total: list.length,
    plus: list.filter(i => (i.point ?? 0) > 0).length,
    minus: list.filter(i => (i.point ?? 0) < 0).length
  }
})

const filters = computed(() => {
  const list = appStore.scoreItems
  return [
    { key: 'all', label: '全部', count: list.length },
    { key: 'plus', label: '加分', count: list.filter(i => (i.point ?? 0) > 0).length },
    { key: 'minus', label: '减分', count: list.filter(i => (i.point ?? 0) < 0).length }
  ]
})

const filtered = computed(() => {
  let list = appStore.scoreItems
  if (filter.value === 'plus') list = list.filter(i => (i.point ?? 0) > 0)
  else if (filter.value === 'minus') list = list.filter(i => (i.point ?? 0) < 0)

  const kw = keyword.value.trim().toLowerCase()
  if (kw) list = list.filter(i => (i.name || '').toLowerCase().includes(kw))
  return list
})

const pointPreview = computed(() => {
  const p = Number(form.point)
  if (!Number.isFinite(p) || p === 0) return '±0'
  return p > 0 ? `+${p}` : `${p}`
})

function resetForm() {
  form.icon = ''
  form.name = ''
  form.point = 1
  formError.value = ''
}

function openCreate() {
  editing.value = null
  resetForm()
  showForm.value = true
  nextTick(() => document.querySelector('.emoji-row .input')?.focus())
}

function openEdit(item) {
  editing.value = item
  form.icon = item.icon || ''
  form.name = item.name || ''
  form.point = item.point ?? 1
  formError.value = ''
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editing.value = null
  submitting.value = false
}

function stepPoint(delta) {
  const cur = Number(form.point)
  const next = (Number.isFinite(cur) ? cur : 0) + delta
  form.point = Math.max(-99, Math.min(99, next))
}

async function submit() {
  formError.value = ''
  const icon = (form.icon || '').trim()
  const name = (form.name || '').trim()
  const point = Number(form.point)

  if (!icon) { formError.value = '选一个图标（或直接输入 emoji）'; return }
  if (!name) { formError.value = '填一下名称'; return }
  if (!Number.isFinite(point) || point === 0) { formError.value = '分值不能为 0'; return }
  if (Math.abs(point) > 99) { formError.value = '分值范围是 -99 ~ 99'; return }

  submitting.value = true
  try {
    if (editing.value) {
      await appStore.updateScoreItem(editing.value.id, icon, name, Math.trunc(point))
      toast(`已更新「${name}」`)
    } else {
      await appStore.createScoreItem(icon, name, Math.trunc(point))
      toast(`已添加「${name}」${point > 0 ? '+' : ''}${point}`)
    }
    closeForm()
  } catch (e) {
    formError.value = e.response?.data?.error || e.message || '保存失败'
  } finally {
    submitting.value = false
  }
}

async function remove(item) {
  const ok = await $confirm.confirm(
    `删除「${item.name}」后，加减分弹窗里不再出现它；已经记下的加减分历史不受影响。`,
    { title: '删除评分项', okText: '删除', icon: '🗑️', danger: true }
  )
  if (!ok) return
  try {
    await appStore.deleteScoreItem(item.id)
    toast(`已删除「${item.name}」`)
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message)
  }
}

onMounted(() => {
  appStore.fetchScoreItems().catch(() => {})
})
</script>

<style scoped>
.page-wrap { max-width: 1000px; margin: 0 auto; padding-bottom: 40px; }

/* 搜索 */
.search { position: relative; }
.search input {
  width: 210px;
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
.search input:focus { outline: 0; width: 250px; border-color: var(--brand); box-shadow: 0 0 0 3px var(--brand-soft); }
.search .s-ico { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); font-size: 12px; opacity: .45; }

/* 概览 */
.stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
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
  padding: 10px 14px;
  margin-bottom: 14px;
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  background: var(--surface);
  font-size: 12px;
  line-height: 1.7;
  color: var(--ink-3);
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
.order-hint { font-size: 11.5px; color: var(--ink-4); }

/* 卡片 */
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 12px; }
.scard {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-s);
  transition: box-shadow .18s, border-color .18s;
}
.scard:hover { box-shadow: var(--shadow-m); }
.sc-ico {
  width: 42px; height: 42px;
  flex: 0 0 42px;
  display: flex; align-items: center; justify-content: center;
  font-size: 21px;
  border-radius: 12px;
  background: var(--line-soft);
}
.scard.plus .sc-ico { background: rgba(16, 185, 129, .12); }
.scard.minus .sc-ico { background: rgba(239, 68, 68, .12); }

.sc-main { flex: 1; min-width: 0; }
.sc-main b {
  display: block;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sc-main span { font-size: 11px; color: var(--ink-3); }

.sc-pt {
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  padding: 3px 10px;
  border-radius: 8px;
}
.scard.plus .sc-pt { color: #0F9D6E; background: rgba(16, 185, 129, .10); }
.scard.minus .sc-pt { color: #DC2626; background: rgba(239, 68, 68, .10); }

.sc-actions { display: flex; gap: 4px; }
.row-btn {
  width: 30px; height: 30px;
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

/* 弹窗 */
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
  max-width: 430px;
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

/* 预览 */
.preview {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 14px;
  border-radius: var(--r-sm);
  background: var(--line-soft);
  border: 1px solid var(--line);
}
.pv-ico {
  width: 38px; height: 38px;
  flex: 0 0 38px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
  border-radius: 10px;
  background: var(--surface);
}
.pv-main { flex: 1; min-width: 0; }
.pv-main b {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pv-main span { font-size: 10.5px; color: var(--ink-3); }
.pv-pt { font-size: 15px; font-weight: 700; font-variant-numeric: tabular-nums; }
.preview.plus .pv-pt { color: #0F9D6E; }
.preview.minus .pv-pt { color: #DC2626; }

.field { margin-bottom: 14px; }
.field > label { display: block; font-size: 12.5px; color: var(--ink-2); margin-bottom: 6px; }
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

.emoji-row { display: flex; flex-direction: column; gap: 8px; }
.emoji-input { width: 64px; text-align: center; font-size: 18px; }
.emoji-grid { display: flex; flex-wrap: wrap; gap: 4px; }
.emoji-btn {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  font-size: 16px;
  cursor: pointer;
  transition: .14s;
}
.emoji-btn:hover { background: var(--line-soft); }
.emoji-btn.on { border-color: var(--brand); background: var(--brand-soft); }

.point-row { display: flex; align-items: center; gap: 8px; }
.point-input { width: 84px; text-align: center; font-size: 15px; font-weight: 600; }
.step-btn {
  width: 40px; height: 40px;
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  background: var(--surface);
  color: var(--ink-2);
  font-size: 16px;
  cursor: pointer;
  transition: .14s;
}
.step-btn:hover { background: var(--line-soft); }
.point-hint { font-size: 11px; color: var(--ink-3); }

.pt-chips { display: flex; gap: 6px; margin-top: 8px; flex-wrap: wrap; }
.pt-chip {
  padding: 5px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface);
  font-size: 12.5px;
  color: var(--ink-2);
  cursor: pointer;
  font-family: inherit;
  transition: .14s;
}
.pt-chip:hover { background: var(--line-soft); }
.pt-chip.on { border-color: var(--brand); background: var(--brand-soft); color: var(--brand-deep); font-weight: 600; }
.pt-chip.on.neg { border-color: #FCA5A5; background: rgba(239, 68, 68, .10); color: #DC2626; }

.error-msg { font-size: 12px; color: #DC2626; margin-top: 4px; }

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

/* 响应式 */
@media (max-width: 900px) {
  .stats { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .stat .v { font-size: 20px; }
}
@media (max-width: 768px) {
  .search { width: 100%; }
  .search input { width: 100%; }
  .search input:focus { width: 100%; }
  .create-label { display: none; }
  .create-btn { padding: 9px 12px; font-size: 15px; }
  .order-hint { display: none; }
  .grid { grid-template-columns: 1fr; }
}
</style>
