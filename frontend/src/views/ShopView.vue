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

      <!-- 商品卡片 -->
      <div v-if="items.length === 0" class="empty-tip">
        暂无商品，点击"添加商品"创建第一个商品
      </div>
      
      <div v-else class="items-grid">
        <div v-for="item in items" :key="item.id" class="item-card">
          <div class="item-icon">{{ item.icon }}</div>
          <div class="item-name">{{ item.name }}</div>
          <div class="item-desc" v-if="item.description">{{ item.description }}</div>
          <div class="item-price">💰 {{ item.price }} 粮食</div>
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
          <div class="modal-actions">
            <button class="cancel-btn" @click="closeModal">取消</button>
            <button class="save-btn" @click="saveItem">保存</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 兑换记录 -->
    <div v-if="activeTab === 'records'" class="records-content">
      <div v-if="records.length === 0" class="empty-tip">
        暂无兑换记录
      </div>
      
      <div v-else class="records-list">
        <div v-for="record in records" :key="record.id" class="record-item">
          <div class="record-icon">{{ record.itemIcon }}</div>
          <div class="record-info">
            <div class="record-student">{{ record.studentName }}</div>
            <div class="record-item-name">兑换了 {{ record.itemName }}</div>
          </div>
          <div class="record-food">-{{ record.foodSpent }} 粮食</div>
          <div class="record-time">{{ formatTime(record.createdAt) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/index.js'
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
  stock: 10
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
    const res = await api.get('/shop/records')
    records.value = res.data
  } catch (e) {
    console.error(e)
  }
}

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
    alert('保存失败: ' + (e.response?.data?.message || e.message))
  }
}

const editItem = (item) => {
  editingItem.value = item
  itemForm.value = { ...item }
  showAddModal.value = true
}

const deleteItem = async (id) => {
  if (!confirm('确定删除此商品?')) return
  try {
    await api.delete(`/shop/items/${id}`)
    fetchItems()
  } catch (e) {
    alert('删除失败')
  }
}

const closeModal = () => {
  showAddModal.value = false
  editingItem.value = null
  itemForm.value = { name: '', icon: '', price: 10, description: '', stock: 10 }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getMonth()+1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2,'0')}`
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
</style>