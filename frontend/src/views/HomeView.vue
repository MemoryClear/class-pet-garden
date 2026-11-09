<template>
  <AppShell title="宠物墙" :subtitle="subtitle">
    <!-- 顶栏搜索 -->
    <template #search>
      <div class="search">
        <span class="s-ico">🔍</span>
        <input v-model="searchKw" type="text" placeholder="搜索学生姓名 / 学号">
      </div>
    </template>

    <!-- 顶栏操作 -->
    <template #actions>
      <div class="guide-wrap"><LevelGuide /></div>
      <button class="btn-primary add-btn" @click="router.push('/students')">＋ 添加学生</button>
    </template>

    <!-- 班级概览 -->
    <section class="stats">
      <div class="stat accent">
        <div class="k">👥 学生总数</div>
        <div class="v">{{ stats.total }}<i>人</i></div>
        <div class="d">已领养 {{ stats.adopted }} · 待领养 {{ stats.total - stats.adopted }}</div>
      </div>
      <div class="stat">
        <div class="k">🍖 班级口粮</div>
        <div class="v">{{ stats.food }}</div>
        <div class="d">全班累计食物</div>
      </div>
      <div class="stat">
        <div class="k">⭐ 平均等级</div>
        <div class="v">{{ stats.avgLevel }}</div>
        <div class="d">满级需要 200 分</div>
      </div>
      <div class="stat">
        <div class="k">👑 满级伙伴</div>
        <div class="v">{{ stats.maxLevel }}<i>位</i></div>
        <div class="d">Lv.6 传说形态</div>
      </div>
    </section>

    <!-- 筛选 / 排序 -->
    <section class="toolbar">
      <button
        v-for="f in filters"
        :key="f.key"
        class="chip"
        :class="{ on: filter === f.key }"
        @click="filter = f.key"
      >
        {{ f.label }}<b>{{ f.count }}</b>
      </button>
      <div class="tool-right">
        <span class="tool-label">排序</span>
        <div class="sort">
          <button
            v-for="s in sorts"
            :key="s.key"
            :class="{ on: sortBy === s.key }"
            @click="sortBy = s.key"
          >{{ s.label }}</button>
        </div>
      </div>
    </section>

    <!-- 空状态 -->
    <div v-if="appStore.students.length === 0" class="empty-state">
      <div class="empty-icon">📄</div>
      <div class="empty-text">还没有学生名单</div>
      <div class="empty-hint">先建好班级名单，学生就能领养自己的伙伴了</div>
      <button class="btn-primary" @click="router.push('/students')">去添加学生</button>
    </div>

    <div v-else-if="filteredStudents.length === 0" class="empty-state">
      <div class="empty-icon">🔍</div>
      <div class="empty-text">没有找到匹配的学生</div>
      <div class="empty-hint">试试换个关键词，或切换到「全部」</div>
    </div>

    <!-- 宠物卡片墙 -->
    <div v-else class="pet-container">
      <PetCard
        v-for="stu in filteredStudents"
        :key="stu.id"
        :student="stu"
        @click="openScoreModal(stu)"
        @adopt="openAdoptModal(stu)"
        @exchange="openExchangeModal(stu)"
        @history="openHistoryDetail(stu)"
        @edit="openEditModal(stu)"
        @pokemon="openPokemonModal(stu)"
      />
    </div>

    <!-- 弹窗 -->
    <ScoreModal v-if="scoreModalVisible" :student="currentStudent" @close="scoreModalVisible = false" />
    <PetSelectModal v-if="adoptModalVisible" :student="currentStudent" @close="adoptModalVisible = false" />
    <ExchangeModal v-if="exchangeModalVisible" :student="currentStudent" @close="exchangeModalVisible = false" @exchanged="onExchanged" />
    <StudentEditModal v-if="editModalVisible" :student="currentEditingStudent" @close="editModalVisible = false" @updated="onStudentUpdated" />
    <StudentDetailModal v-if="detailModalVisible" :student="currentStudent" @close="detailModalVisible = false" />
    <StudentPokemonModal v-if="pokemonModalVisible" :student="currentStudent" @close="pokemonModalVisible = false" @updated="onStudentUpdated" />
  </AppShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'
import { useAuthStore } from '../stores/auth.js'
import { useRouter } from 'vue-router'
import AppShell from '../components/layout/AppShell.vue'
import PetCard from '../components/PetCard.vue'
import ScoreModal from '../components/ScoreModal.vue'
import PetSelectModal from '../components/PetSelectModal.vue'
import ExchangeModal from '../components/ExchangeModal.vue'
import StudentDetailModal from '../components/StudentDetailModal.vue'
import StudentEditModal from '../components/StudentEditModal.vue'
import StudentPokemonModal from '../components/StudentPokemonModal.vue'
import LevelGuide from '../components/LevelGuide.vue'
import { levelOf, MAX_LEVEL } from '../constants/petLevels.js'

const appStore = useAppStore()
const authStore = useAuthStore()
const router = useRouter()

const searchKw = ref('')
const filter = ref('all')
const sortBy = ref('food')

const scoreModalVisible = ref(false)
const adoptModalVisible = ref(false)
const exchangeModalVisible = ref(false)
const editModalVisible = ref(false)
const pokemonModalVisible = ref(false)
const detailModalVisible = ref(false)
const currentEditingStudent = ref(null)
const currentStudent = ref(null)

const stats = computed(() => {
  const list = appStore.students
  const total = list.length
  const adopted = list.filter(s => s.petId).length
  const food = list.reduce((sum, s) => sum + (s.food ?? 0), 0)
  const levelSum = list.reduce((sum, s) => sum + levelOf(s.food), 0)
  const maxLevel = list.filter(s => levelOf(s.food) >= MAX_LEVEL).length
  return {
    total,
    adopted,
    food: food.toLocaleString('zh-CN'),
    avgLevel: total ? (levelSum / total).toFixed(1) : '0.0',
    maxLevel
  }
})

const subtitle = computed(() => {
  const n = appStore.students.length
  if (!n) return '先添加学生，再开始养宠物'
  return `${n} 位同学 · 点击卡片即可加减分`
})

const filteredStudents = computed(() => {
  let list = appStore.students
  if (filter.value === 'adopted') list = list.filter(s => s.petId)
  else if (filter.value === 'unadopted') list = list.filter(s => !s.petId)
  else if (filter.value === 'max') list = list.filter(s => levelOf(s.food) >= MAX_LEVEL)

  const kw = searchKw.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(s =>
      (s?.name ?? '').toLowerCase().includes(kw) ||
      (s?.studentNo ?? '').toLowerCase().includes(kw)
    )
  }

  const arr = [...list]
  if (sortBy.value === 'food') arr.sort((a, b) => (b.food ?? 0) - (a.food ?? 0))
  else if (sortBy.value === 'level') arr.sort((a, b) => levelOf(b.food) - levelOf(a.food) || (b.food ?? 0) - (a.food ?? 0))
  else arr.sort((a, b) => (a.name || '').localeCompare(b.name || '', 'zh-CN'))
  return arr
})

const filters = computed(() => {
  const list = appStore.students
  return [
    { key: 'all', label: '全部', count: list.length },
    { key: 'adopted', label: '已领养', count: list.filter(s => s.petId).length },
    { key: 'unadopted', label: '待领养', count: list.filter(s => !s.petId).length },
    { key: 'max', label: '👑 满级', count: list.filter(s => levelOf(s.food) >= MAX_LEVEL).length }
  ]
})

const sorts = [
  { key: 'food', label: '积分' },
  { key: 'level', label: '等级' },
  { key: 'name', label: '姓名' }
]

function openScoreModal(stu) {
  currentStudent.value = stu
  scoreModalVisible.value = true
}

function openAdoptModal(stu) {
  currentStudent.value = stu
  adoptModalVisible.value = true
}

function openExchangeModal(stu) {
  currentStudent.value = stu
  exchangeModalVisible.value = true
}

function openEditModal(stu) {
  currentEditingStudent.value = stu
  editModalVisible.value = true
}

function openPokemonModal(stu) {
  currentStudent.value = stu
  pokemonModalVisible.value = true
}

function openHistoryDetail(stu) {
  currentStudent.value = stu
  detailModalVisible.value = true
}

function onStudentUpdated(updated) {
  const idx = appStore.students.findIndex(s => s.id === updated.id)
  if (idx !== -1) {
    appStore.students[idx] = { ...appStore.students[idx], ...updated }
  }
}

async function onExchanged() {
  await appStore.fetchStudents()
  if (currentStudent.value) {
    const updated = appStore.students.find(s => s.id === currentStudent.value.id)
    if (updated) currentStudent.value = updated
  }
}

onMounted(async () => {
  await appStore.fetchStudents()
  await appStore.fetchScoreItems()
  await appStore.fetchSettings()
  await appStore.fetchPetLibrary()
  if (!appStore.shopItems.length) appStore.fetchShopItems()
  if (authStore.isLoggedIn) {
    document.title = `${appStore.settings.systemName} - 课堂宠物乐园`
  }
})
</script>

<style scoped>
/* ---------- 搜索框 ---------- */
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

.add-btn { padding: 9px 16px; }

/* ---------- 概览卡 ---------- */
.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}
.stat {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  padding: 13px 15px;
  box-shadow: var(--shadow-s);
  min-width: 0;
}
.stat .k { font-size: 11.5px; color: var(--ink-3); }
.stat .v {
  font-size: 23px;
  font-weight: 700;
  letter-spacing: -.02em;
  margin-top: 6px;
  font-variant-numeric: tabular-nums;
}
.stat .v i { font-style: normal; font-size: 12px; font-weight: 600; color: var(--ink-3); margin-left: 3px; }
.stat .d { font-size: 11px; color: var(--ink-3); margin-top: 3px; }
.stat.accent {
  background: linear-gradient(140deg, var(--brand-soft), rgba(255, 255, 255, .4));
  border-color: var(--brand-soft-2);
}

/* ---------- 工具条 ---------- */
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
}
.chip:hover { border-color: #E4DAD7; }
.chip b { margin-left: 4px; font-weight: 600; opacity: .55; }
.chip.on { background: var(--pill-active-bg); border-color: var(--pill-active-bg); color: var(--pill-active-fg); font-weight: 600; }
.chip.on b { opacity: .7; }

.tool-right { margin-left: auto; display: flex; align-items: center; gap: 8px; }
.tool-label { font-size: 12px; color: var(--ink-3); }
.sort { display: flex; background: var(--surface); border: 1px solid var(--line); border-radius: 9px; padding: 3px; }
.sort button {
  border: 0;
  background: transparent;
  padding: 4px 10px;
  border-radius: 7px;
  font-size: 12px;
  color: #7A8190;
  cursor: pointer;
  transition: .16s;
}
.sort button.on { background: var(--line-soft); color: var(--ink); font-weight: 600; }

/* ---------- 卡片墙 ---------- */
.pet-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(176px, 1fr));
  gap: 14px;
}

/* ---------- 空状态 ---------- */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: var(--surface);
  border: 1px dashed var(--line);
  border-radius: var(--r-lg);
}
.empty-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 14px;
  border-radius: 50%;
  background: var(--line-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
}
.empty-text { font-size: 15px; font-weight: 600; color: var(--ink); margin-bottom: 6px; }
.empty-hint { font-size: 12.5px; color: var(--ink-3); margin-bottom: 18px; }

/* ---------- 响应式 ---------- */
@media (max-width: 1100px) {
  .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 768px) {
  .search { width: 100%; }
  .search input { width: 100%; height: 34px; }
  .search input:focus { width: 100%; }
  .guide-wrap { display: none; }
  .add-btn { display: none; }
  .stats { gap: 8px; margin-bottom: 12px; }
  .stat { padding: 10px 12px; }
  .stat .v { font-size: 19px; }
  .toolbar { gap: 6px; flex-wrap: wrap; }
  .chip { flex: 0 0 auto; padding: 6px 11px; font-size: 12px; }
  .tool-right { margin-left: auto; }
  .tool-label { display: none; }
  .pet-container { grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 10px; }
  .empty-state { padding: 40px 16px; }
}
</style>
