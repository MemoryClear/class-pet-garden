<template>
  <div class="home-page" :style="themeStyle">
    <div class="container">
      <div class="navbar">
        <div class="navbar-left">
          <div class="class-info">
            <span class="class-icon">🏫</span>
            <span>{{ appStore.settings.className }}</span>
          </div>
          <input type="text" class="search-box" v-model="searchKw" placeholder="搜索学生...">
        </div>
        <div class="navbar-right">
          <button class="nav-btn" @click="router.push('/leaderboard')">🏆 光荣榜</button>
          <button class="nav-btn" @click="router.push('/shop')">🏪 小卖部</button>
          <LevelGuide />
          <button class="nav-btn" @click="router.push('/history')">📜 历史</button>
          <button class="nav-btn" @click="router.push('/exchange-history')">📦 明细</button>
          <button class="nav-btn" @click="router.push('/settings')">⚙️ 设置</button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="appStore.students.length === 0" class="empty-state">
        <div class="empty-icon">📄</div>
        <div class="empty-text">暂无学生名单</div>
        <a class="empty-link" @click="router.push('/settings')">去设置添加学生</a>
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
        />
      </div>
    </div>

    <!-- 加减分弹窗 -->
    <ScoreModal v-if="scoreModalVisible" :student="currentStudent" @close="scoreModalVisible = false" />
    <!-- 宠物选择弹窗 -->
    <PetSelectModal v-if="adoptModalVisible" :student="currentStudent" @close="adoptModalVisible = false" />
    <!-- 兑换商品弹窗 -->
    <ExchangeModal v-if="exchangeModalVisible" :student="currentStudent" @close="exchangeModalVisible = false" @exchanged="onExchanged" />
    <!-- 学生明细弹窗 -->
    <StudentDetailModal v-if="detailModalVisible" :student="currentStudent" @close="detailModalVisible = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'
import { useAuthStore } from '../stores/auth.js'
import { useRouter } from 'vue-router'
import PetCard from '../components/PetCard.vue'
import ScoreModal from '../components/ScoreModal.vue'
import PetSelectModal from '../components/PetSelectModal.vue'
import ExchangeModal from '../components/ExchangeModal.vue'
import StudentDetailModal from '../components/StudentDetailModal.vue'
import LevelGuide from '../components/LevelGuide.vue'

const appStore = useAppStore()
const authStore = useAuthStore()
const router = useRouter()

const searchKw = ref('')
const scoreModalVisible = ref(false)
const adoptModalVisible = ref(false)
const exchangeModalVisible = ref(false)
const detailModalVisible = ref(false)
const currentStudent = ref(null)

const themeMap = {
  pink:'rgba(255,107,157,0.08)', purple:'rgba(168,85,247,0.08)',
  indigo:'rgba(139,92,246,0.08)', blue:'rgba(59,130,246,0.08)',
  cyan:'rgba(34,211,238,0.08)', teal:'rgba(20,184,166,0.08)',
  green:'rgba(34,197,94,0.08)', yellow:'rgba(234,179,8,0.08)',
  orange:'rgba(249,115,22,0.08)', red:'rgba(244,63,94,0.08)'
}

const themeStyle = computed(() => ({
  backgroundColor: themeMap[appStore.settings.theme] || themeMap.pink,
  minHeight: '100vh'
}))

const filteredStudents = computed(() => {
  if (!searchKw.value) return appStore.students
  const kw = searchKw.value.toLowerCase()
  return appStore.students.filter(s => s.name.toLowerCase().includes(kw))
})

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

function openHistoryDetail(stu) {
  currentStudent.value = stu
  detailModalVisible.value = true
}

async function onExchanged() {
  // 刷新学生数据以更新 equippedItems
  await appStore.fetchStudents()
  // 更新 currentStudent 为最新数据
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
  if (authStore.isLoggedIn) {
    document.title = `${appStore.settings.systemName} - 课堂宠物乐园`
  }
})
</script>

<style scoped>
.home-page { min-height:100vh; }
.container { max-width:1000px; margin:0 auto; padding:20px; }
.navbar { background:#fff; border-radius:24px; box-shadow:0 2px 8px rgba(0,0,0,0.06); padding:10px 20px; display:flex; align-items:center; gap:16px; margin-bottom:40px; flex-wrap:wrap; }
.navbar-left { display:flex; align-items:center; gap:8px; flex:1; }
.class-info { display:flex; align-items:center; gap:6px; background:#fff0f3; padding:6px 12px; border-radius:16px; color:#ff4d79; font-size:14px; font-weight:500; cursor:pointer; }
.class-icon { width:24px; height:24px; background:#ff4d79; border-radius:50%; display:flex; align-items:center; justify-content:center; color:#fff; font-size:12px; }
.search-box { flex:1; max-width:240px; padding:8px 12px 8px 32px; border:1px solid #f0f0f0; border-radius:16px; font-size:14px; background:#fff; }
.navbar-right { display:flex; gap:8px; }
.nav-btn { background:transparent; border:none; padding:8px 12px; border-radius:12px; cursor:pointer; font-size:14px; }
.empty-state { text-align:center; padding:60px 20px; border-radius:24px; }
.empty-icon { width:80px; height:80px; background:#f7f3f0; border-radius:50%; display:flex; align-items:center; justify-content:center; margin:0 auto 16px; font-size:32px; }
.empty-text { font-size:16px; color:#666; margin-bottom:12px; }
.empty-link { color:#ff4d79; cursor:pointer; font-size:14px; }
.pet-container { display:grid; grid-template-columns:repeat(auto-fill, minmax(220px, 1fr)); gap:20px; }
</style>