<template>
  <div class="student-home" :style="bgStyle">
    <div class="container">
      <!-- 顶部导航 -->
      <div class="navbar">
        <div class="navbar-left">
          <span class="greeting">👋 {{ authStore.user?.studentName || myInfo.name || '同学' }}</span>
          <span class="student-no">学号: {{ authStore.user?.studentNo || myInfo.studentNo || '无' }}</span>
        </div>
        <div class="navbar-right">
          <button class="nav-btn" @click="activeSection='pet'">🐾 我的宠物</button>
          <button class="nav-btn" @click="activeSection='leaderboard'">🏆 光荣榜</button>
          <button class="nav-btn" @click="activeSection='shop'">🏪 小卖部</button>
          <button class="nav-btn" @click="activeSection='classroom'">📚 课堂</button>
          <button class="nav-btn" @click="activeSection='records'">📝 记录</button>
          <button class="nav-btn" @click="activeSection='level'">📊 等级说明</button>
          <button class="nav-btn logout-btn" @click="handleLogout">退出</button>
        </div>
      </div>

      <!-- 我的宠物 -->
      <div v-if="activeSection === 'pet'" class="section pet-section">
        <div class="pet-card-large">
          <div class="pet-icon">{{ myInfo.petIcon || '🥚' }}</div>
          <div class="pet-info">
            <h2>{{ myInfo.petName || '还没有宠物' }}</h2>
            <div class="pet-level">{{ myInfo.level }}</div>
            <div class="pet-score">
              <span class="score-label">积分</span>
              <span class="score-value">{{ myInfo.food }}</span>
            </div>
            <div v-if="myInfo.petChangeCards > 0" class="pet-cards">
              🃏 宠物更换卡 x{{ myInfo.petChangeCards }}
            </div>
          </div>
        </div>
        <!-- 宠物图鉴 -->
        <div class="pet-library">
          <h3>🐾 宠物图鉴</h3>
          <div class="pet-grid">
            <div v-for="p in petLibrary" :key="p.id" class="pet-thumb" :class="{ owned: myInfo.petId === p.id }">
              <span class="thumb-icon">{{ p.icon }}</span>
              <span class="thumb-name">{{ p.name }}</span>
              <button v-if="myInfo.petId !== p.id" class="adopt-btn" @click="adoptPet(p)">
                {{ myInfo.petId ? '更换' : '领养' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 光荣榜 -->
      <div v-if="activeSection === 'leaderboard'" class="section">
        <div class="section-tabs">
          <button :class="{ active: lbTab === 'food' }" @click="lbTab='food'">零食排行</button>
          <button :class="{ active: lbTab === 'total' }" @click="lbTab='total'">总积分排行</button>
        </div>
        <div v-if="lbTab === 'food'" class="leaderboard-list">
          <div v-for="(s, i) in leaderboard" :key="s.id" class="lb-item" :class="{ me: s.id === myInfo.id }">
            <span class="lb-rank">{{ i < 3 ? ['🥇','🥈','🥉'][i] : (i+1) }}</span>
            <span class="lb-icon">{{ s.petIcon || '🥚' }}</span>
            <span class="lb-name">{{ s.name }}</span>
            <span class="lb-score">{{ s.food }}分</span>
          </div>
        </div>
        <div v-if="lbTab === 'total'" class="leaderboard-list">
          <div v-for="(s, i) in totalLeaderboard" :key="s.id" class="lb-item" :class="{ me: s.id === myInfo.id }">
            <span class="lb-rank">{{ i < 3 ? ['🥇','🥈','🥉'][i] : (i+1) }}</span>
            <span class="lb-icon">{{ s.petIcon || '🥚' }}</span>
            <span class="lb-name">{{ s.name }}</span>
            <span class="lb-score">{{ s.totalScore }}分</span>
          </div>
        </div>
      </div>

      <!-- 小卖部 -->
      <div v-if="activeSection === 'shop'" class="section">
        <div class="shop-header">
          <span>💰 我的积分: {{ myInfo.food }}</span>
        </div>
        <div class="shop-grid">
          <div v-for="item in shopItems" :key="item.id" class="shop-item">
            <span class="item-icon">{{ item.icon }}</span>
            <span class="item-name">{{ item.name }}</span>
            <span class="item-price">{{ item.price }}分</span>
            <button class="exchange-btn" :disabled="myInfo.food < item.price" @click="exchangeItem(item)">
              兑换
            </button>
          </div>
        </div>
      </div>

      <!-- 课堂 -->
      <div v-if="activeSection === 'classroom'" class="section">
        <ClassroomView />
      </div>

      <!-- 记录 -->
      <div v-if="activeSection === 'records'" class="section">
        <div class="section-tabs">
          <button :class="{ active: recordTab === 'score' }" @click="recordTab='score'">💰 积分明细</button>
          <button :class="{ active: recordTab === 'exchange' }" @click="recordTab='exchange'">🎁 道具明细</button>
        </div>
        <div v-if="recordTab === 'score'" class="records-list">
          <div v-for="r in scoreHistory" :key="r.id" class="record-item">
            <div class="record-info">
              <span class="record-icon">{{ r.scoreItemIcon || '📝' }}</span>
              <div class="record-detail">
                <span class="record-name">{{ r.scoreItemName }}</span>
                <span v-if="r.revoked" class="revoked-tag">已撤销</span>
              </div>
            </div>
            <div class="record-right">
              <span :class="['record-points', r.point >= 0 ? 'positive' : 'negative']">
                {{ r.point >= 0 ? '+' : '' }}{{ r.point }}
              </span>
              <span class="record-time">{{ formatTime(r.createdAt) }}</span>
            </div>
          </div>
          <div v-if="scoreHistory.length === 0" class="empty">暂无积分记录</div>
        </div>
        <div v-if="recordTab === 'exchange'" class="records-list">
          <div v-for="r in exchangeHistory" :key="r.id" class="record-item">
            <div class="record-info">
              <span class="record-icon">{{ r.itemIcon }}</span>
              <div class="record-detail">
                <span class="record-name">{{ r.itemName }}</span>
                <span v-if="r.giftFromName" class="gift-tag">🎁 来自 {{ r.giftFromName }}</span>
              </div>
            </div>
            <div class="record-right">
              <span class="record-points negative">-{{ r.foodSpent }}</span>
              <span class="record-time">{{ formatTime(r.createdAt) }}</span>
            </div>
          </div>
          <div v-if="exchangeHistory.length === 0" class="empty">暂无道具记录</div>
        </div>
      </div>

      <!-- 等级说明 -->
      <div v-if="activeSection === 'level'" class="section">
        <h3 class="section-title">📊 宠物等级说明</h3>
        <div class="level-list">
          <div v-for="lv in levelGuide" :key="lv.level" class="level-item">
            <div class="level-name">{{ lv.level }}</div>
            <div class="level-range">{{ lv.range }}</div>
            <div class="level-desc">{{ lv.desc }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useAuthStore } from '../stores/auth.js'
import { useRouter } from 'vue-router'
import api, { studentApi2 } from '../api/index.js'
import ClassroomView from './ClassroomView.vue'

const authStore = useAuthStore()
const router = useRouter()

const activeSection = ref('pet')
const lbTab = ref('food')
const myInfo = reactive({})
const leaderboard = ref([])
const totalLeaderboard = ref([])
const petLibrary = ref([])
const shopItems = ref([])
const levelGuide = ref([])
const recordTab = ref('score')
const scoreHistory = ref([])
const exchangeHistory = ref([])

const bgStyle = computed(() => ({
  backgroundColor: 'rgba(255,107,157,0.06)',
  minHeight: '100vh'
}))

async function fetchMyInfo() {
  try {
    const { data } = await api.get('/student/me')
    Object.assign(myInfo, data)
  } catch (e) {
    console.error('fetchMyInfo error', e)
  }
}

async function fetchLeaderboard() {
  try {
    const [r1, r2] = await Promise.all([
      api.get('/student/leaderboard'),
      api.get('/student/leaderboard/total')
    ])
    leaderboard.value = r1.data
    totalLeaderboard.value = r2.data
  } catch (e) {
    console.error('fetchLeaderboard error', e)
  }
}

async function fetchShop() {
  try {
    const { data } = await api.get('/student/shop')
    shopItems.value = data
  } catch (e) {
    console.error('fetchShop error', e)
  }
}

async function fetchLevelGuide() {
  try {
    const { data } = await api.get('/student/level-guide')
    levelGuide.value = data
  } catch (e) {
    console.error('fetchLevelGuide error', e)
  }
}

async function fetchPetLibrary() {
  try {
    const { data } = await api.get('/student/pets')
    petLibrary.value = data
  } catch (e) {
    console.error('fetchPetLibrary error', e)
  }
}

async function adoptPet(pet) {
  if (myInfo.petId && myInfo.petChangeCards <= 0) {
    alert('更换宠物需要先购买宠物更换卡！')
    return
  }
  if (myInfo.petId && !confirm('更换宠物需要消耗一张宠物更换卡，确定继续吗？')) return
  try {
    await studentApi2.adopt({ petId: pet.id, petName: pet.name, petIcon: pet.icon })
    alert('领养成功！🐾')
    await fetchMyInfo()
  } catch (e) {
    alert(e.response?.data?.error || '领养失败')
  }
}

async function exchangeItem(item) {
  if (!confirm(`确定花费 ${item.price} 分兑换「${item.name}」吗？`)) return
  try {
    await studentApi2.exchange(item.id)
    alert('兑换成功！')
    await fetchMyInfo()
    await fetchShop()
  } catch (e) {
    alert(e.response?.data?.error || '兑换失败')
  }
}

function formatTime(t) {
  if (!t) return ''
  const s = typeof t === 'string' ? t : t.toString()
  return s.replace('T', ' ').substring(0, 16)
}

async function fetchRecords() {
  try {
    const [r1, r2] = await Promise.all([
      api.get('/student/score-history'),
      api.get('/student/exchange-history')
    ])
    scoreHistory.value = r1.data || []
    exchangeHistory.value = r2.data || []
  } catch (e) {
    console.error('fetchRecords error', e)
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

onMounted(async () => {
  await Promise.all([fetchMyInfo(), fetchLeaderboard(), fetchPetLibrary(), fetchShop(), fetchLevelGuide(), fetchRecords()])
})
</script>

<style scoped>
.student-home { min-height: 100vh; }
.container { max-width: 800px; margin: 0 auto; padding: 20px; }
.navbar { background: #fff; border-radius: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); padding: 12px 20px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-bottom: 24px; }
.navbar-left { display: flex; align-items: center; gap: 12px; }
.greeting { font-weight: 600; font-size: 1.1rem; }
.student-no { background: #f0f0f0; padding: 4px 12px; border-radius: 12px; font-size: 0.85rem; color: #666; }
.navbar-right { display: flex; gap: 6px; flex-wrap: wrap; }
.nav-btn { background: transparent; border: none; padding: 6px 12px; border-radius: 10px; cursor: pointer; font-size: 0.9rem; }
.nav-btn:hover { background: #f5f5f5; }
.logout-btn { color: #f44; }
.section { background: #fff; border-radius: 20px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }

/* 宠物卡片 */
.pet-card-large { display: flex; align-items: center; gap: 24px; padding: 20px; background: linear-gradient(135deg, #fff5f7, #fff); border-radius: 16px; border: 2px solid #ffe0e8; }
.pet-icon { font-size: 64px; }
.pet-info h2 { margin: 0 0 8px; font-size: 1.4rem; }
.pet-level { display: inline-block; padding: 4px 14px; background: #fff0f3; color: #ff6b9d; border-radius: 12px; font-size: 0.9rem; margin-bottom: 10px; }
.pet-score { display: flex; align-items: baseline; gap: 6px; }
.score-label { color: #888; font-size: 0.9rem; }
.score-value { font-size: 2rem; font-weight: 700; color: #ff6b9d; }
.pet-cards { margin-top: 8px; color: #8b5cf6; font-size: 0.9rem; }
.pet-library { margin-top: 20px; }
.pet-library h3 { margin-bottom: 12px; }
.pet-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(80px, 1fr)); gap: 10px; }
.pet-thumb { text-align: center; padding: 10px; border-radius: 12px; border: 2px solid #f0f0f0; transition: all 0.2s; }
.pet-thumb.owned { border-color: #ff6b9d; background: #fff5f7; }
.thumb-icon { font-size: 28px; display: block; }
.thumb-name { font-size: 0.75rem; color: #666; }
.adopt-btn { display: block; width: 100%; margin-top: 4px; padding: 2px 6px; background: #ff6b9d; color: #fff; border: none; border-radius: 6px; font-size: 0.7rem; cursor: pointer; }

/* 光荣榜 */
.section-tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.section-tabs button { padding: 8px 18px; border: 1px solid #ddd; border-radius: 16px; background: #fff; cursor: pointer; }
.section-tabs button.active { background: #ff6b9d; color: #fff; border-color: #ff6b9d; }
.leaderboard-list { }
.lb-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border-radius: 12px; margin-bottom: 6px; }
.lb-item.me { background: #fff0f3; border: 2px solid #ff6b9d; }
.lb-rank { width: 32px; text-align: center; font-weight: 600; }
.lb-icon { font-size: 24px; }
.lb-name { flex: 1; }
.lb-score { font-weight: 600; color: #ff6b9d; }

/* 小卖部 */
.shop-header { margin-bottom: 16px; font-size: 1.1rem; font-weight: 600; }
.shop-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; }
.shop-item { background: #fafafa; border-radius: 12px; padding: 14px; text-align: center; }
.item-icon { font-size: 28px; display: block; margin-bottom: 6px; }
.item-name { display: block; font-weight: 500; margin-bottom: 4px; }
.item-price { display: block; color: #ff6b9d; font-size: 0.9rem; margin-bottom: 8px; }
.exchange-btn { padding: 6px 16px; background: #ff6b9d; color: #fff; border: none; border-radius: 12px; cursor: pointer; }
.exchange-btn:disabled { background: #ddd; cursor: not-allowed; }

/* 等级说明 */
.section-title { margin-bottom: 16px; }
.level-list { display: grid; gap: 12px; }
.level-item { display: flex; align-items: center; gap: 16px; padding: 14px 18px; background: #fafafa; border-radius: 12px; }
.level-name { font-size: 1.1rem; font-weight: 600; min-width: 120px; }
.level-range { color: #ff6b9d; font-weight: 500; min-width: 80px; }
.level-desc { color: #666; }

/* 记录 */
.records-list { max-height: 500px; overflow-y: auto; }
.record-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; border-radius: 12px; margin-bottom: 6px; background: #fafafa; transition: all 0.2s; }
.record-item:hover { background: #f5f5f5; }
.record-info { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.record-icon { font-size: 1.2rem; }
.record-detail { display: flex; flex-direction: column; min-width: 0; }
.record-name { font-weight: 500; font-size: 0.95rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.record-right { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
.record-points { font-weight: 700; font-size: 1.1rem; }
.record-points.positive { color: #059669; }
.record-points.negative { color: #dc2626; }
.record-time { font-size: 0.75rem; color: #999; }
.revoked-tag { color: #999; font-size: 0.8rem; text-decoration: line-through; }
.gift-tag { color: #8b5cf6; font-size: 0.85rem; }
</style>
