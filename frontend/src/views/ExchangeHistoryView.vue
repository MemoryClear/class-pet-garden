<template>
  <div class="exchange-history-page">
    <div class="header-bar">
      <button class="back-btn" @click="router.push('/home')">← 返回</button>
      <h2>📦 商品兑换明细</h2>
    </div>
    <div class="container">
      <!-- Tab 切换（按动作类型） -->
      <div class="tab-bar">
        <button :class="{ active: viewTab === 'all' }" @click="viewTab = 'all'">📋 全部</button>
        <button :class="{ active: viewTab === 'PURCHASE' }" @click="viewTab = 'PURCHASE'">🛒 兑换</button>
        <button :class="{ active: viewTab === 'GIFT_OUT' }" @click="viewTab = 'GIFT_OUT'">📤 赠出</button>
        <button :class="{ active: viewTab === 'GIFT_IN' }" @click="viewTab = 'GIFT_IN'">📥 收到</button>
        <button :class="{ active: viewTab === 'REVOKED' }" @click="viewTab = 'REVOKED'">🚫 撤销</button>
      </div>

      <!-- 筛选 -->
      <div class="filter-bar">
        <select v-model="filterStudentId" class="filter-select">
          <option value="">全部学生</option>
          <option v-for="stu in appStore.students" :key="stu.id" :value="stu.id">{{ stu.name }}</option>
        </select>
      </div>

      <!-- 4 张统计卡 -->
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-num">{{ stats.total }}</div>
          <div class="stat-lbl">总记录</div>
        </div>
        <div class="stat-card cost">
          <div class="stat-num">💰 {{ stats.cost }}</div>
          <div class="stat-lbl">总消耗</div>
        </div>
        <div class="stat-card gift-out">
          <div class="stat-num">{{ stats.giftOut }}</div>
          <div class="stat-lbl">赠出次数</div>
        </div>
        <div class="stat-card gift-in">
          <div class="stat-num">{{ stats.giftIn }}</div>
          <div class="stat-lbl">收到次数</div>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <div class="loading-icon">⏳</div>
        <div>加载中...</div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="filteredRecords.length === 0" class="empty-state">
        <div class="empty-icon">📦</div>
        <div class="empty-text">{{ emptyText }}</div>
      </div>

      <!-- 时间线列表 -->
      <div v-else class="timeline">
        <div v-for="group in groupedByDate" :key="group.date" class="day-group">
          <div class="day-header">{{ group.date }}</div>
          <div v-for="record in group.records" :key="record.id" class="record-item" :class="{ gifted: isGiftedOut(record) }">
            <div class="record-icon">{{ getItemIcon(record.itemId) }}</div>
            <div class="record-info">
              <div class="record-name">
                {{ record.studentName }}
                <span v-if="record.actionType === 'GIFT_OUT' && record.giftToName" class="badge gift-out">📤 赠送给 {{ record.giftToName }}</span>
                <span v-else-if="record.actionType === 'GIFT_IN' && record.giftFromName" class="badge gift-in">📥 来自 {{ record.giftFromName }}</span>
                <span v-else-if="record.actionType === 'PURCHASE'" class="badge purchase">🛒 兑换</span>
                <span v-else-if="record.actionType === 'REVOKED'" class="badge revoked">🚫 已撤销</span>
                <span v-if="isGiftedOut(record)" class="not-owned-tag" :title="'已赠送给 ' + record.giftToName + '，非本人持有'">非本人持有</span>
              </div>
              <div class="record-item-name">{{ getItemName(record.itemId) }}</div>
            </div>
            <div class="record-price" :class="{ zero: !record.foodSpent }">
              <span v-if="record.foodSpent > 0">💰 {{ record.foodSpent }}</span>
              <span v-else-if="record.actionType === 'GIFT_OUT'">赠出</span>
              <span v-else-if="record.actionType === 'GIFT_IN'">收到</span>
              <span v-else>—</span>
            </div>
            <div class="record-time">{{ formatTime(record.exchangeTime || record.createdAt) }}</div>
            <button
              v-if="record.actionType === 'PURCHASE' || record.actionType === 'GIFT_OUT'"
              class="revoke-btn"
              :disabled="isGiftedOut(record)"
              :title="isGiftedOut(record) ? '该道具已赠送给「' + record.giftToName + '」，无法撤销' : ''"
              @click="revokeRecord(record)">
              撤销
            </button>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="!loading && (appStore.exchangeRecordsHasMore || appStore.exchangeRecordsLoading)" class="load-more">
        <button v-if="appStore.exchangeRecordsHasMore" class="load-more-btn" :disabled="appStore.exchangeRecordsLoading" @click="loadMore">
          {{ appStore.exchangeRecordsLoading ? '加载中…' : '加载更多' }}
        </button>
        <span v-else class="load-more-end">— 已加载全部 —</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'
import { useRouter, useRoute } from 'vue-router'
import api from '../api/index.js'
import $confirm from '../composables/useConfirmModal.js'

const appStore = useAppStore()
const router = useRouter()
const route = useRoute()
const loading = ref(true)
const viewTab = ref('all')
const filterStudentId = ref('')

// 商品信息
function getItemName(itemId) {
  const item = appStore.shopItems.find(i => i.id === itemId)
  return item?.name || '未知商品'
}

// 撤销兑换
async function revokeRecord(record) {
  if (!record.studentName || !record.itemName) return
  if (!await $confirm.confirm(`确定撤销「${record.studentName}」兑换的「${record.itemName}」吗？将退还 ${record.foodSpent} 粮食。`)) return
  try {
    await api.post(`/shop/records/${record.id}/revoke`)
    $confirm.success('已撤销')
    // 刷新数据
    await appStore.fetchExchangeRecords({ reset: true, limit: 20 })
    await appStore.fetchStudents()
  } catch (e) {
    $confirm.error('撤销失败: ' + (e.response?.data?.error || e.message))
  }
}
function getItemIcon(itemId) {
  const item = appStore.shopItems.find(i => i.id === itemId)
  return item?.icon || '❓'
}

// 是否已送出（非本人持有）
function isGiftedOut(record) {
  return record.actionType === 'GIFT_OUT' && !!record.giftToName
}

// 格式化
function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}
function formatDateHeader(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')}`
}

// 读取记录（兼容数组 / {items} 两种形态）
function readRecords() {
  const recs = appStore.exchangeRecords
  return Array.isArray(recs) ? recs : (recs?.items || [])
}

// 筛选
const filteredRecords = computed(() => {
  let records = readRecords()
  if (viewTab.value !== 'all') {
    records = records.filter(r => (r.actionType || 'PURCHASE') === viewTab.value)
  }
  if (filterStudentId.value) {
    records = records.filter(r => r.studentId === filterStudentId.value)
  }
  return [...records].sort((a, b) => {
    const ta = new Date(a.exchangeTime || a.createdAt).getTime()
    const tb = new Date(b.exchangeTime || b.createdAt).getTime()
    return tb - ta
  })
})

// 按日期分组
const groupedByDate = computed(() => {
  const groups = {}
  for (const r of filteredRecords.value) {
    const date = formatDateHeader(r.exchangeTime || r.createdAt)
    if (!groups[date]) groups[date] = []
    groups[date].push(r)
  }
  return Object.entries(groups).map(([date, records]) => ({ date, records }))
})

// 统计卡
const stats = computed(() => {
  const all = readRecords()
  return {
    total: all.length,
    cost: all.filter(r => (r.actionType || 'PURCHASE') === 'PURCHASE').reduce((s, r) => s + (r.foodSpent || 0), 0),
    giftOut: all.filter(r => r.actionType === 'GIFT_OUT').length,
    giftIn: all.filter(r => r.actionType === 'GIFT_IN').length
  }
})

const emptyText = computed(() => {
  const map = {
    PURCHASE: '暂无兑换记录',
    GIFT_OUT: '暂无赠出记录',
    GIFT_IN: '暂无收到记录',
    REVOKED: '暂无撤销记录'
  }
  return map[viewTab.value] || '暂无商品记录'
})

// 初始化
onMounted(async () => {
  try {
    if (appStore.students.length === 0) await appStore.fetchStudents()
    await appStore.fetchShopItems()
    await appStore.fetchExchangeRecords({ reset: true })
    if (route.query.studentId) filterStudentId.value = route.query.studentId
  } finally {
    loading.value = false
  }
})

async function loadMore() {
  if (!appStore.exchangeRecordsHasMore || appStore.exchangeRecordsLoading) return
  await appStore.fetchExchangeRecords({ reset: false })
}
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
  flex-wrap: wrap;
}

.tab-bar button {
  padding: 6px 14px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 18px;
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
.filter-bar { margin-bottom: 16px; }
.filter-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  min-width: 160px;
}

/* 4 张统计卡 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 12px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.stat-card.cost { background: linear-gradient(135deg, #fef3c7, #fde68a); }
.stat-card.gift-out { background: linear-gradient(135deg, #dbeafe, #bfdbfe); }
.stat-card.gift-in { background: linear-gradient(135deg, #fce7f3, #fbcfe8); }
.stat-num { font-size: 18px; font-weight: 700; color: #2d3748; }
.stat-lbl { font-size: 11px; color: #6b7280; margin-top: 2px; }

/* 时间线 */
.timeline { display: flex; flex-direction: column; gap: 16px; }
.day-group {
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.day-header {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 4px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #f3f4f6;
}
.record-item:last-child { border-bottom: none; }

.record-icon { font-size: 26px; }
.record-info { flex: 1; min-width: 0; }
.record-name {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
}
.badge.purchase { background: #ecfdf5; color: #059669; }
.badge.gift-out { background: #dbeafe; color: #2563eb; }
.badge.gift-in { background: #fce7f3; color: #db2777; }
.badge.revoked { background: #f3f4f6; color: #6b7280; text-decoration: line-through; }

.record-item-name {
  font-size: 12px;
  color: #718096;
  margin-top: 2px;
}

.record-price {
  font-size: 13px;
  font-weight: 600;
  color: #f59e0b;
  min-width: 56px;
  text-align: right;
}
.record-price.zero { color: #9ca3af; font-weight: 500; }

.record-time {
  font-size: 11px;
  color: #9ca3af;
  min-width: 40px;
  text-align: right;
}

.revoke-btn {
  margin-left: 8px;
  padding: 3px 10px;
  font-size: 11px;
  border: 1px solid #fca5a5;
  background: white;
  color: #dc2626;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.revoke-btn:hover:not(:disabled) {
  background: #dc2626;
  color: white;
}
.revoke-btn:disabled {
  background: #f5f5f5;
  color: #b0b0b0;
  border-color: #e0e0e0;
  cursor: not-allowed;
}

/* 已赠送的兑换记录：灰色 + 不透明度 */
.record-item.gifted {
  opacity: 0.7;
  background: #fafafa;
}
.record-item.gifted .record-name {
  color: #6b7280;
}
.not-owned-tag {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 6px;
  font-size: 10px;
  background: #e5e7eb;
  color: #6b7280;
  border-radius: 8px;
  font-weight: normal;
  white-space: nowrap;
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

.load-more { text-align: center; margin-top: 16px; }
.load-more-btn { background:#fff; border:1px solid #fdb2a4; color:#f97316; padding:8px 24px; border-radius:8px; font-size:14px; cursor:pointer; transition:all 0.2s; }
.load-more-btn:hover:not(:disabled) { background:#fff5f1; }
.load-more-btn:disabled { opacity:0.6; cursor:not-allowed; }
.load-more-end { color:#999; font-size:12px; }
</style>