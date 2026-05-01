<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-card">
      <div class="modal-header">
        <div class="modal-title">📊 {{ student.name }} 的明细</div>
        <button class="modal-close" @click="emit('close')">×</button>
      </div>

      <!-- Tab 切换 -->
      <div class="tab-bar">
        <button class="tab-btn" :class="{ active: tab === 'score' }" @click="tab = 'score'">
          📈 积分明细
        </button>
        <button class="tab-btn" :class="{ active: tab === 'item' }" @click="tab = 'item'">
          🎒 道具明细
          <span class="tab-badge">{{ myItems.length }}</span>
        </button>
      </div>

      <!-- 积分明细 -->
      <div v-if="tab === 'score'" class="tab-content">
        <!-- 统计 -->
        <div class="stat-bar">
          <span class="stat-item">当前积分：<b>{{ student.food ?? 0 }}</b></span>
          <span class="stat-item">累计得分：<b>{{ totalEarned }}</b></span>
          <span class="stat-item">累计消耗：<b>{{ totalSpent }}</b></span>
        </div>

        <!-- 积分来源筛选 -->
        <div class="filter-bar">
          <button class="filter-btn" :class="{ active: scoreFilter === 'all' }" @click="scoreFilter = 'all'">全部</button>
          <button class="filter-btn" :class="{ active: scoreFilter === 'score' }" @click="scoreFilter = 'score'">📝 评分</button>
          <button class="filter-btn" :class="{ active: scoreFilter === 'exchange' }" @click="scoreFilter = 'exchange'">🛒 兑换</button>
        </div>

        <!-- 列表 -->
        <div class="record-list">
          <div v-if="filteredScores.length === 0" class="empty-tip">暂无记录</div>
          <div v-for="rec in filteredScores" :key="rec.id" class="record-item" :class="{ revoked: rec.revoked }">
            <div class="record-icon">{{ getScoreIcon(rec) }}</div>
            <div class="record-main">
              <div class="record-title">
                <span class="point" :class="rec.point > 0 ? 'positive' : 'negative'">
                  {{ rec.point > 0 ? '+' : '' }}{{ rec.point }}
                </span>
                <span class="reason">{{ formatScoreReason(rec) }}</span>
                <span v-if="rec.revoked" class="revoked-tag">已撤销</span>
              </div>
              <div class="record-time">{{ formatTime(rec.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 道具明细 -->
      <div v-if="tab === 'item'" class="tab-content">
        <!-- 统计 -->
        <div class="stat-bar">
          <span class="stat-item">持有：<b>{{ ownedCount }}</b></span>
          <span class="stat-item">已装备：<b>{{ equippedCount }}</b></span>
          <span class="stat-item">已转出：<b>{{ giftedCount }}</b></span>
        </div>

        <!-- 道具来源筛选 -->
        <div class="filter-bar">
          <button class="filter-btn" :class="{ active: itemFilter === 'all' }" @click="itemFilter = 'all'">全部</button>
          <button class="filter-btn" :class="{ active: itemFilter === 'exchange' }" @click="itemFilter = 'exchange'">🛒 兑换</button>
          <button class="filter-btn" :class="{ active: itemFilter === 'received' }" @click="itemFilter = 'received'">🎁 收取</button>
          <button class="filter-btn" :class="{ active: itemFilter === 'gifted' }" @click="itemFilter = 'gifted'">🎁 赠送</button>
        </div>

        <!-- 列表 -->
        <div class="record-list">
          <div v-if="filteredItems.length === 0" class="empty-tip">暂无道具</div>
          <div v-for="rec in filteredItems" :key="rec.id" class="record-item item-record">
            <div class="record-icon">{{ getItemIcon(rec.itemId) }}</div>
            <div class="record-main">
              <div class="record-title">
                <span class="item-name">{{ rec.itemName }}</span>
                <span class="item-price">-{{ rec.foodSpent }}🍖</span>
                <span v-if="rec.giftFromName" class="gift-tag">🎁 来自 {{ rec.giftFromName }}</span>
                <span v-else-if="isGiftedOut(rec.id)" class="gift-out-tag">🎁 转给 {{ getGiftedToName(rec.id) }}</span>
                <span v-if="isEquipped(rec.itemId)" class="equipped-tag">✅ 已装备</span>
              </div>
              <div class="record-time">{{ formatTime(rec.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'

const props = defineProps({ student: { type: Object, required: true } })
const emit = defineEmits(['close'])
const appStore = useAppStore()

const tab = ref('score')
const scoreFilter = ref('all')
const itemFilter = ref('all')

// ============== 积分数据 ==============
const scoreRecords = computed(() => {
  return appStore.history
    .filter(r => r.studentId === props.student.id)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

// 筛选积分记录
const filteredScores = computed(() => {
  let records = scoreRecords.value
  if (scoreFilter.value === 'score') {
    // 评分操作：不含"购买"的
    records = records.filter(r => !r.scoreItemName?.includes('购买'))
  } else if (scoreFilter.value === 'exchange') {
    // 道具兑换：含"购买"
    records = records.filter(r => r.scoreItemName?.includes('购买'))
  }
  return records
})

// 累计得分（加分）
const totalEarned = computed(() => {
  return scoreRecords.value
    .filter(r => r.point > 0 && !r.revoked)
    .reduce((sum, r) => sum + r.point, 0)
})

// 累计消耗（扣分）
const totalSpent = computed(() => {
  return scoreRecords.value
    .filter(r => r.point < 0 && !r.revoked)
    .reduce((sum, r) => sum + r.point, 0)
})

// 格式化积分原因
function formatScoreReason(rec) {
  if (!rec.scoreItemName) return '评分'
  if (rec.scoreItemName.includes('购买')) {
    return rec.scoreItemName  // 如 "📦 购买「蝴蝶结」"
  }
  // 评分操作显示
  return rec.scoreItemName
}

// 获取积分图标
function getScoreIcon(rec) {
  if (rec.scoreItemName?.includes('购买')) return '🛒'
  return rec.point > 0 ? '📝' : '❌'
}

// ============== 道具数据 ==============
// 持有的道具（当前学生是owner的）
const myItems = computed(() => {
  return appStore.exchangeRecords
    .filter(r => r.studentId === props.student.id)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

// 我 转给别人的记录（通过gift功能转出）
async function getGiftedOutRecords() {
  // 需要查所有记录，找giftFrom=当前学生ID的
  return appStore.exchangeRecords.filter(r => 
    r.giftFrom === props.student.id
  )
}

// 筛选道具记录
const filteredItems = computed(() => {
  let records = myItems.value
  if (itemFilter.value === 'exchange') {
    // 自己兑换的（无giftFrom）
    records = records.filter(r => !r.giftFrom)
  } else if (itemFilter.value === 'received') {
    // 收到的（有giftFrom）
    records = records.filter(r => r.giftFrom)
  } else if (itemFilter.value === 'gifted') {
    // 转给别人的（giftFrom=当前学生ID）
    records = appStore.exchangeRecords
      .filter(r => r.giftFrom === props.student.id)
  }
  return records
})

// 持有数
const ownedCount = computed(() => myItems.value.length)

// 已装备数
const equippedCount = computed(() => {
  const ids = parseEquippedIds()
  return myItems.value.filter(rec => ids.includes(rec.itemId)).length
})

// 已转出数
const giftedCount = computed(() => {
  return appStore.exchangeRecords.filter(r => r.giftFrom === props.student.id).length
})

// 判断某条记录是否已转出
function isGiftedOut(recordId) {
  const record = appStore.exchangeRecords.find(r => r.id === recordId)
  return record?.giftFrom === props.student.id
}

// 获取转给谁
function getGiftedToName(recordId) {
  const record = appStore.exchangeRecords.find(r => r.id === recordId)
  return record?.studentName || ''
}

// 解析已装备商品ID
function parseEquippedIds() {
  try {
    const json = props.student.equippedItems
    if (!json) return []
    return JSON.parse(json)
  } catch { return [] }
}

// 判断是否已装备
function isEquipped(itemId) {
  return parseEquippedIds().includes(itemId)
}

// 获取商品图标
function getItemIcon(itemId) {
  const item = appStore.shopItems.find(i => i.id === itemId)
  return item?.icon || '📦'
}

// 格式化时间
function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(async () => {
  if (!appStore.history.length) await appStore.fetchHistory()
  if (!appStore.exchangeRecords.length) await appStore.fetchExchangeRecords()
  if (!appStore.shopItems.length) await appStore.fetchShopItems()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-card {
  background: #fff;
  border-radius: 16px;
  width: 420px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.modal-close {
  width: 28px;
  height: 28px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  color: #666;
}
.modal-close:hover { background: #eee; }

.tab-bar {
  display: flex;
  padding: 12px 20px;
  gap: 8px;
  border-bottom: 1px solid #eee;
}

.tab-btn {
  flex: 1;
  padding: 8px 12px;
  border: none;
  background: #f5f5f5;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.tab-badge {
  background: #ff9500;
  color: white;
  font-size: 11px;
  padding: 1px 5px;
  border-radius: 10px;
}

.tab-content {
  padding: 16px 20px;
  overflow-y: auto;
  flex: 1;
}

.stat-bar {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-item {
  font-size: 13px;
  color: #666;
}
.stat-item b {
  color: #333;
  font-size: 15px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
  font-size: 14px;
}

.record-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
  transition: background 0.2s;
}
.record-item:hover { background: #f0f0f0; }
.record-item.revoked {
  opacity: 0.5;
  background: #f5f5f5;
}

.record-icon {
  font-size: 20px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 8px;
  flex-shrink: 0;
}

.record-main {
  flex: 1;
  min-width: 0;
}

.record-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.point {
  font-weight: 700;
  font-size: 15px;
}
.point.positive { color: #52c41a; }
.point.negative { color: #ff4d4f; }

.reason {
  color: #333;
  font-size: 13px;
}

.revoked-tag {
  background: #ff4d4f;
  color: white;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}

.record-time {
  font-size: 12px;
  color: #999;
}

.item-record .record-icon {
  font-size: 24px;
}

.item-name {
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.item-price {
  color: #ff9500;
  font-size: 13px;
}

.gift-tag {
  background: #ff85c0;
  color: white;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}

.equipped-tag {
  background: #52c41a;
  color: white;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}

.gift-out-tag {
  background: #fa8c16;
  color: white;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}

.filter-bar {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 4px 10px;
  border: none;
  background: #eee;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}
.filter-btn.active {
  background: #667eea;
  color: white;
}
.filter-btn:hover:not(.active) { background: #ddd; }
</style>
