<template>
  <div class="shop-page">
    <div class="header-bar">
      <button class="back-btn" @click="router.push('/home')">← 返回</button>
      <h1 class="page-title">🏪 小卖部</h1>
    </div>
    
    <!-- 切换标签 -->
    <div class="shop-tabs">
      <button 
        :class="['tab-btn', { active: activeTab === 'items' }]" 
        @click="activeTab = 'items'"
      >
        📦 商品列表
      </button>
      <button 
        :class="['tab-btn', { active: activeTab === 'records' }]" 
        @click="activeTab = 'records'"
      >
        📜 兑换记录
      </button>
    </div>

    <!-- 商品列表 -->
    <div v-if="activeTab === 'items'" class="shop-content">
      <!-- 添加商品按钮 -->
      <button class="add-btn" @click="showAddModal = true">
        ➕ 添加商品
      </button>

      <!-- 搜索 + 分类筛选 -->
    <div v-if="items.length > 0" class="filter-bar">
      <input
        v-model="searchKeyword"
        type="text"
        class="search-input"
        placeholder="🔍 搜索商品名称或描述..."
      />
      <div class="category-tabs">
        <button :class="['cat-tab', { active: categoryFilter === 'all' }]" @click="categoryFilter = 'all'">全部</button>
        <button :class="['cat-tab', { active: categoryFilter === 'decoration' }]" @click="categoryFilter = 'decoration'">🎀 装饰</button>
        <button :class="['cat-tab', { active: categoryFilter === 'evolution_item' }]" @click="categoryFilter = 'evolution_item'">✨ 进化道具</button>
        <button :class="['cat-tab', { active: categoryFilter === 'pet_change_card' }]" @click="categoryFilter = 'pet_change_card'">🔄 更换卡</button>
        <button :class="['cat-tab', { active: categoryFilter === 'pokemon_ball' }]" @click="categoryFilter = 'pokemon_ball'">⚪ 精灵球</button>
      </div>
      <div class="filter-summary" v-if="searchKeyword || categoryFilter !== 'all'">
        <span class="match-count">匹配 {{ filteredItems.length }} / {{ items.length }} 件</span>
        <button class="clear-filter" @click="clearFilters">✕ 清空筛选</button>
      </div>
    </div>

    <!-- 商品卡片 -->
    <div v-if="items.length === 0" class="empty-tip">
      暂无商品，点击"添加商品"创建第一个商品
    </div>
    <div v-else-if="filteredItems.length === 0" class="empty-tip">
      没有匹配的商品
    </div>

    <div v-else class="items-grid">
      <div v-for="item in filteredItems" :key="item.id" class="item-card">
          <div class="item-icon">{{ item.icon }}</div>
          <div class="item-name">{{ item.name }}</div>
          <div class="item-desc" v-if="item.description">{{ item.description }}</div>
          <div class="item-price">💰 {{ item.price }} 粮食</div>
          <div class="item-type-badge" :class="'type-' + (item.itemType || 'decoration')">
            {{ itemTypeLabel(item.itemType) }}
          </div>
          <div class="item-stock" :class="{ 'low-stock': item.stock <= 3 }">
            📦 库存: {{ item.stock }}
          </div>
          <div class="item-actions">
            <button class="edit-btn" @click="editItem(item)">✏️</button>
            <button class="delete-btn" @click="deleteItem(item.id)">🗑️</button>
          </div>
        </div>
      </div>

      <!-- 添加/编辑弹窗 -->
      <div v-if="showAddModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal">
          <h2>{{ editingItem ? '✏️ 编辑商品' : '➕ 添加商品' }}</h2>
          <div class="form-group">
            <label>商品名称</label>
            <input v-model="itemForm.name" placeholder="如: 棒棒糖" />
          </div>
          <div class="form-group">
            <label>图标</label>
            <input v-model="itemForm.icon" placeholder="如: 🍭" />
          </div>
          <div class="form-group">
            <label>价格 (粮食)</label>
            <input v-model.number="itemForm.price" type="number" min="1" />
          </div>
          <div class="form-group">
            <label>描述</label>
            <input v-model="itemForm.description" placeholder="可选描述" />
          </div>
          <div class="form-group">
            <label>库存</label>
            <input v-model.number="itemForm.stock" type="number" min="0" />
          </div>
          <div class="form-group">
            <label>商品类型</label>
            <select v-model="itemForm.itemType" class="form-select">
              <option value="decoration">装饰道具</option>
              <option value="pet_change_card">宠物更换卡</option>
              <option value="pokemon_ball">精灵球</option>
              <option value="evolution_item">进化道具</option>
            </select>
          </div>
          <div v-if="itemForm.itemType === 'evolution_item'" class="form-group">
            <label>进化道具Key</label>
            <select v-model="itemForm.evolutionItemKey" class="form-select">
              <option value="">请选择</option>
              <option value="水之石">水之石</option>
              <option value="火之石">火之石</option>
              <option value="叶之石">叶之石</option>
              <option value="月之石">月之石</option>
              <option value="雷之石">雷之石</option>
              <option value="联系绳">联系绳</option>
              <option value="日之石">日之石</option>
              <option value="黑奇石">黑奇石</option>
              <option value="冰之石">冰之石</option>
            </select>
          </div>
          <div class="modal-actions">
            <button class="cancel-btn" @click="closeModal">取消</button>
            <button class="save-btn" @click="saveItem">保存</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 兑换记录 -->
    <div v-if="activeTab === 'records'" class="records-content">
      <div v-if="filteredRecords.length === 0 && !recordsLoading" class="empty-tip">
        暂无兑换记录
      </div>

      <div v-else class="records-list">
        <div v-for="record in filteredRecords" :key="record.id" class="record-item">
          <div class="record-icon">{{ record.itemIcon }}</div>
          <div class="record-info">
            <div class="record-student">{{ record.studentName }}</div>
            <div class="record-item-name">兑换了 {{ record.itemName }}</div>
          </div>
          <div class="record-food">-{{ record.foodSpent }} 粮食</div>
          <div class="record-time">{{ formatTime(record.createdAt) }}</div>
          <button class="revoke-btn" @click="revokeRecord(record)">撤销</button>
        </div>
      </div>

      <div v-if="recordsHasMore" class="load-more">
        <button :disabled="recordsLoading" @click="loadMoreRecords">{{ recordsLoading ? '加载中…' : '加载更多' }}</button>
      </div>
      <div v-else-if="filteredRecords.length > 0" class="end-marker">— 已加载全部 —</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/index.js'
import $confirm from '../composables/useConfirmModal.js'

const router = useRouter()
const items = ref([])
const records = ref([])
const activeTab = ref('items')
const showAddModal = ref(false)
const editingItem = ref(null)

const itemForm = ref({
  name: '',
  icon: '',
  price: 10,
  description: '',
  stock: 10,
  itemType: 'decoration',
  evolutionItemKey: ''
})

const fetchItems = async () => {
  try {
    const res = await api.get('/shop/items')
    items.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const fetchRecords = async () => {
  try {
    recordsLoading.value = true
    // 小卖部兑换记录只需要 PURCHASE（教师兑换道具的记录），不含赠送
    const res = await api.get('/shop/records', { params: { limit: 500 } })
    const data = res.data || {}
    const items = Array.isArray(data) ? data : (data.items || [])
    records.value = items
    recordsHasMore.value = !!(data.hasMore && items.length >= 500)
  } catch (e) {
    console.error(e)
  } finally {
    recordsLoading.value = false
  }
}

const revokeRecord = async (record) => {
  if (!record.studentName || !record.itemName) return
  if (!await $confirm.confirm(`确定撤销「${record.studentName}」兑换的「${record.itemName}」吗？将退还 ${record.foodSpent} 粮食。`)) return
  try {
    await api.post(`/shop/records/${record.id}/revoke`)
    $confirm.success('已撤销')
    await fetchRecords()
    await appStore.fetchStudents()
  } catch (e) {
    $confirm.error('撤销失败: ' + (e.response?.data?.error || e.message))
  }
}

const loadMoreRecords = async () => { /* 不需 loadMore，小卖部记录一次性加载 */ }
const recordsLoading = ref(false)
const recordsHasMore = ref(false)
const recordsNextCursor = ref(null)
const filteredRecords = computed(() => {
  // 只显示 PURCHASE（教师兑换道具的记录），过滤掉赠送 (GIFT_OUT / GIFT_IN)
  return records.value.filter(r => (r.actionType || 'PURCHASE') === 'PURCHASE')
})

const saveItem = async () => {
  try {
    if (editingItem.value) {
      await api.put(`/shop/items/${editingItem.value.id}`, itemForm.value)
    } else {
      await api.post('/shop/items', itemForm.value)
    }
    closeModal()
    fetchItems()
  } catch (e) {
    $confirm.error('保存失败: ' + (e.response?.data?.message || e.message))
  }
}

const editItem = (item) => {
  editingItem.value = item
  itemForm.value = { ...item }
  showAddModal.value = true
}

const deleteItem = async (id) => {
  if (!await $confirm.confirm('确定删除此商品?')) return
  try {
    await api.delete(`/shop/items/${id}`)
    fetchItems()
  } catch (e) {
    $confirm.error('删除失败')
  }
}

const closeModal = () => {
  showAddModal.value = false
  editingItem.value = null
  itemForm.value = { name: '', icon: '', price: 10, description: '', stock: 10, itemType: 'decoration', evolutionItemKey: '' }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getMonth()+1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2,'0')}`
}

const itemTypeLabel = (type) => {
  const labels = {
    decoration: '装饰',
    pet_change_card: '更换卡',
    pokemon_ball: '精灵球',
    evolution_item: '进化道具'
  }
  return labels[type] || '装饰'
}

const searchKeyword = ref('')
const categoryFilter = ref('all')

const filteredItems = computed(() => {
  let list = items.value
  if (categoryFilter.value !== 'all') {
    list = list.filter(i => (i.itemType || 'decoration') === categoryFilter.value)
  }
  const kw = searchKeyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(i => (i.name || '').toLowerCase().includes(kw) ||
                             (i.description || '').toLowerCase().includes(kw))
  }
  return list
})

const clearFilters = () => {
  searchKeyword.value = ''
  categoryFilter.value = 'all'
}

watch(activeTab, (tab) => {
  if (tab === 'items') fetchItems()
  else if (tab === 'records') fetchRecords()
})

onMounted(() => {
  fetchItems()
})
</script>

<style scoped>
.shop-page {
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);

.filter-bar {
  margin-bottom: 16px;
  padding: 12px 14px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 6px rgba(245, 158, 11, 0.1);
}
.search-input {
  width: 100%;
  padding: 8px 14px;
  border: 2px solid #fbbf24;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  margin-bottom: 10px;
}
.search-input:focus {
  border-color: #ea580c;
  box-shadow: 0 0 0 3px rgba(234, 88, 12, 0.15);
}
.category-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.cat-tab {
  padding: 5px 12px;
  border: 1px solid #fbbf24;
  background: white;
  border-radius: 16px;
  cursor: pointer;
  font-size: 13px;
  color: #92400e;
  transition: all 0.15s;
}
.cat-tab:hover {
  background: #fef3c7;
}
.cat-tab.active {
  background: linear-gradient(135deg, #f59e0b, #ea580c);
  color: white;
  border-color: #ea580c;
  font-weight: 600;
}
.filter-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #92400e;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #fde68a;
}
.match-count { font-weight: 600; }
.clear-filter {
  background: none;
  border: none;
  color: #b45309;
  cursor: pointer;
  font-size: 12px;
  text-decoration: underline;
  padding: 0;
}
}

.header-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.back-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 20px;
  background: rgba(255,255,255,0.6);
  cursor: pointer;
  font-size: 1rem;
  color: #92400e;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(255,255,255,0.9);
}

.page-title {
  text-align: center;
  color: #92400e;
  font-size: 2rem;
  flex: 1;
  margin: 0;
}

.shop-tabs {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
}

.tab-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 20px;
  background: rgba(255,255,255,0.5);
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.3s;
}

.tab-btn.active {
  background: #d97706;
  color: white;
}

.add-btn {
  display: block;
  margin: 0 auto 20px;
  padding: 10px 20px;
  border: none;
  border-radius: 20px;
  background: #059669;
  color: white;
  cursor: pointer;
  font-size: 1rem;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 15px;
}

.item-card {
  background: white;
  border-radius: 15px;
  padding: 15px;
  text-align: center;
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}

.item-icon {
  font-size: 3rem;
}

.item-name {
  font-weight: bold;
  margin: 10px 0 5px;
}

.item-desc {
  font-size: 0.8rem;
  color: #666;
  margin-bottom: 5px;
}

.item-price {
  color: #dc2626;
  font-weight: bold;
}

.item-stock {
  font-size: 0.9rem;
  color: #059669;
}

.item-stock.low-stock {
  color: #dc2626;
}

.item-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 10px;
}

.edit-btn, .delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: white;
  border-radius: 15px;
  padding: 25px;
  width: 90%;
  max-width: 400px;
}

.modal h2 {
  margin-bottom: 20px;
  color: #92400e;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  color: #666;
}

.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
}

.form-select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  background: white;
}

.item-type-badge {
  display: inline-block;
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: 10px;
  margin: 4px 0;
}

.type-decoration { background: #e0e7ff; color: #4338ca; }
.type-pet_change_card { background: #fef3c7; color: #92400e; }
.type-pokemon_ball { background: #fee2e2; color: #991b1b; }
.type-evolution_item { background: #d1fae5; color: #065f46; }

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.cancel-btn, .save-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.cancel-btn {
  background: #e5e7eb;
}

.save-btn {
  background: #059669;
  color: white;
}

.empty-tip {
  text-align: center;
  color: #666;
  padding: 40px;
}

.records-list {
  max-width: 600px;
  margin: 0 auto;
}

.record-item {
  display: flex;
  align-items: center;
  background: white;
  border-radius: 10px;
  padding: 15px;
  margin-bottom: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.records-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.rec-tab {
  padding: 8px 16px;
  border-radius: 20px;
  border: 2px solid #e0e0e0;
  background: white;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}
.rec-tab.active {
  background: #ff8c42;
  color: white;
  border-color: #ff8c42;
}

.action-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 0.78rem;
  font-weight: normal;
}
.action-badge.gift-out { background: #dbeafe; color: #1e40af; }
.action-badge.gift-in  { background: #f3e8ff; color: #6b21a8; }
.action-badge.purchase { background: #d1fae5; color: #065f46; }
.action-badge.revoked  { background: #e5e7eb; color: #374151; }

.direction {
  margin-left: 8px;
  font-size: 0.85rem;
  color: #888;
  font-weight: normal;
}

.record-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.gift-out-label {
  color: #1e40af;
  font-weight: bold;
}

.load-more, .end-marker {
  text-align: center;
  padding: 16px;
  color: #999;
}
.load-more button {
  padding: 8px 24px;
  border-radius: 20px;
  border: 2px solid #ff8c42;
  background: white;
  color: #ff8c42;
  cursor: pointer;
}
.load-more button:disabled { opacity: 0.5; cursor: not-allowed; }

.record-icon {
  font-size: 2rem;
  margin-right: 15px;
}

.record-info {
  flex: 1;
}

.record-student {
  font-weight: bold;
  color: #333;
}

.record-item-name {
  font-size: 0.9rem;
  color: #666;
}

.record-food {
  color: #dc2626;
  font-weight: bold;
}

.record-time {
  color: #999;
  font-size: 0.85rem;
}

.revoke-btn {
  padding: 4px 12px;
  font-size: 12px;
  border: 1px solid #fca5a5;
  background: white;
  color: #dc2626;
  border-radius: 14px;
  cursor: pointer;
  margin-left: 8px;
  white-space: nowrap;
  transition: all 0.15s;
}
.revoke-btn:hover {
  background: #dc2626;
  color: white;
}
</style>