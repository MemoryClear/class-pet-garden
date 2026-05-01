<template>
  <div class="exchange-history-page">
    <div class="header-bar">
      <button class="back-btn" @click="router.push('/home')">← 返回</button>
      <h2>📦 商品兑换明细</h2>
    </div>
    <div class="container">
      <!-- Tab 切换 -->
      <div class="tab-bar">
        <button :class="{ active: viewTab === 'all' }" @click="viewTab = 'all'">📋 全部记录</button>
        <button :class="{ active: viewTab === 'exchange' }" @click="viewTab = 'exchange'">🛒 兑换</button>
        <button :class="{ active: viewTab === 'gift' }" @click="viewTab = 'gift'">🎁 赠送</button>
      </div>

      <!-- 筛选 -->
      <div class="filter-bar">
        <select v-model="filterStudentId" class="filter-select">
          <option value="">全部学生</option>
          <option v-for="stu in appStore.students" :key="stu.id" :value="stu.id">{{ stu.name }}</option>
        </select>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <div class="loading-icon">⏳</div>
        <div>加载中...</div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="filteredRecords.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <div class="empty-text">
          {{ viewTab === 'gift' ? '暂无赠送记录' : viewTab === 'exchange' ? '暂无兑换记录' : '暂无商品记录' }}
        </div>
      </div>

      <!-- 记录列表 -->
      <div v-else class="record-list">
        <div v-for="record in filteredRecords" :key="record.id" class="record-item">
          <div class="record-icon">{{ getItemIcon(record.itemId) }}</div>
          <div class="record-info">
            <div class="record-name">
              {{ record.studentName }}
              <span v-if="record.giftFromName" class="gift-from">🎁 来自 {{ record.giftFromName }}</span>
            </div>
            <div class="record-item-name">{{ getItemName(record.itemId) }}</div>
          </div>
          <div class="record-type">
            <span v-if="record.giftFromName" class="type-badge gift">赠送</span>
            <span v-else class="type-badge exchange">兑换</span>
          </div>
          <div class="record-meta">
            <div class="record-time">{{ formatTime(record.exchangeTime) }}</div>
            <div class="record-price">💰 {{ getItemPrice(record.itemId) }}</div>
          </div>
        </div>
      </div>

      <!-- 统计 -->
      <div v-if="!loading && filteredRecords.length > 0" class="stats-bar">
        <div class="stat-item">
          <span class="stat-label">总记录</span>
          <span class="stat-value">{{ filteredRecords.length }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">总消耗</span>
          <span class="stat-value">💰 {{ totalCost }}</span>
        </div>
        <div v-if="viewTab === 'all' || viewTab === 'gift'" class="stat-item">
          <span class="stat-label">赠送次数</span>
          <span class="stat-value">{{ giftCount }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'
import { useRouter, useRoute } from 'vue-router'

const appStore = useAppStore()
const router = useRouter()
const route = useRoute()
const loading = ref(true)
const viewTab = ref('all') // all | exchange | gift
const filterStudentId = ref('')

// 获取商品信息
function getItemName(itemId) {
  const item = appStore.shopItems.find(i => i.id === itemId)
  return item?.name || '未知商品'
}

function getItemIcon(itemId) {
  const item = appStore.shopItems.find(i => i.id === itemId)
  return item?.icon || '❓'
}

function getItemPrice(itemId) {
  const item = appStore.shopItems.find(i => i.id === itemId)
  return item?.price || 0
}

// 格式化时间
function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getMonth()+1}/${d.getDate()} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

// 筛选记录
const filteredRecords = computed(() => {
  let records = appStore.exchangeRecords

  // 按类型筛选
  if (viewTab.value === 'exchange') {
    records = records.filter(r => !r.giftFromName)
  } else if (viewTab.value === 'gift') {
    records = records.filter(r => r.giftFromName)
  }

  // 按学生筛选
  if (filterStudentId.value) {
    records = records.filter(r => r.studentId === filterStudentId.value)
  }

  // 按时间倒序
  return [...records].sort((a, b) => new Date(b.exchangeTime) - new Date(a.exchangeTime))
})

// 统计
const totalCost = computed(() => {
  return filteredRecords.value.reduce((sum, r) => sum + getItemPrice(r.itemId), 0)
})

const giftCount = computed(() => {
  return filteredRecords.value.filter(r => r.giftFromName).length
})

// 初始化
onMounted(async () => {
  try {
    if (appStore.students.length === 0) await appStore.fetchStudents()
    await appStore.fetchShopItems()
    await appStore.fetchExchangeRecords()
    // 从 URL 参数读取学生筛选
    if (route.query.studentId) {
      filterStudentId.value = route.query.studentId
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.exchange-history-page { min-height: 100vh; background: #f7f3f0; }

.header-bar {
  background: #fff;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  margin-bottom: 20px;
}

.back-btn {
  background: #f0f0f0;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

h2 { font-size: 18px; font-weight: 600; color: #2d3748; }

.container { max-width: 800px; margin: 0 auto; padding: 0 20px 40px; }

/* Tab 切换 */
.tab-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.tab-bar button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-bar button.active {
  background: #f59e0b;
  color: #fff;
  border-color: #f59e0b;
  font-weight: 600;
}

/* 筛选 */
.filter-bar { margin-bottom: 20px; }
.filter-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  min-width: 160px;
}

/* 加载/空状态 */
.loading-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
}

.loading-icon, .empty-icon {
  width: 80px;
  height: 80px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 32px;
}

.empty-text { font-size: 16px; color: #666; }

/* 记录列表 */
.record-list {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.record-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.record-item:last-child { border-bottom: none; }

.record-icon { font-size: 28px; }

.record-info { flex: 1; }

.record-name {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  display: flex;
  align-items: center;
  gap: 8px;
}

.gift-from {
  font-size: 11px;
  background: #fce7f3;
  color: #ec4899;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.record-item-name {
  font-size: 12px;
  color: #718096;
  margin-top: 2px;
}

/* 类型标签 */
.record-type { min-width: 50px; }

.type-badge {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 10px;
  font-weight: 600;
}

.type-badge.exchange {
  background: #ecfdf5;
  color: #059669;
}

.type-badge.gift {
  background: #fce7f3;
  color: #ec4899;
}

/* 元信息 */
.record-meta {
  text-align: right;
  min-width: 70px;
}

.record-time {
  font-size: 12px;
  color: #999;
}

.record-price {
  font-size: 12px;
  color: #f59e0b;
  font-weight: 600;
  margin-top: 2px;
}

/* 统计栏 */
.stats-bar {
  margin-top: 20px;
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  gap: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: #2d3748;
}
</style>
