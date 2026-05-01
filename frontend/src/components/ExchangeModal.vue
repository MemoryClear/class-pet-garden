<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal">
      <h2>🏪 兑换商品</h2>
      <div class="student-info">
        <span class="pet-icon">{{ student.petIcon || '❓' }}</span>
        <span class="student-name">{{ student.name }}</span>
        <span class="food-remaining">🍖 剩余: {{ student.food || 0 }}</span>
      </div>

      <!-- 步骤指示 -->
      <div class="step-indicator">
        <span :class="{ active: step === 'select', done: step !== 'select' }">① 选购</span>
        <span class="arrow">→</span>
        <span :class="{ active: step === 'equip', done: step === 'done' }">② 装备</span>
        <span class="arrow">→</span>
        <span :class="{ active: step === 'done' }">③ 完成</span>
      </div>

      <!-- 背包切换 -->
      <div class="tab-bar" v-if="step === 'select'">
        <button :class="{ active: viewTab === 'all' }" @click="viewTab = 'all'">🛍️ 全部商品</button>
        <button :class="{ active: viewTab === 'mine' }" @click="viewTab = 'mine'">🎒 我的道具</button>
      </div>

      <!-- 商品选择 -->
      <div v-if="step === 'select'">
        <!-- 错误提示 -->
        <div v-if="error" class="error-banner" @click="error = null">
          ⚠️ {{ error }}
        </div>

        <!-- 我的道具视图 -->
        <div v-if="viewTab === 'mine'">
          <div v-if="myItems.length === 0" class="empty-inventory">
            <div class="empty-icon">🎒</div>
            <p>还没有兑换过任何道具</p>
            <p class="sub">去「全部商品」兑换吧~</p>
          </div>
          <div v-else class="items-container">
            <div class="items-grid">
            <div
              v-for="item in myItems"
              :key="item.id"
              class="item-card mine-item"
            >
              <div class="item-icon">{{ item.icon }}</div>
              <div class="item-name">{{ item.name }}</div>
              <div v-if="isEquipped(item.id)" class="equipped-badge">✅ 已装备</div>
              <div v-else-if="getGiftFrom(item.id)" class="gift-badge">🎁 来自{{ getGiftFrom(item.id) }}</div>
              <div class="item-actions">
                <button
                  v-if="!isEquipped(item.id)"
                  class="re-equip-btn"
                  @click="quickEquip(item)"
                  :disabled="equippingId === item.id"
                >
                  {{ equippingId === item.id ? '装备中...' : '⚡ 装备' }}
                </button>
                <button
                  v-if="!isEquipped(item.id)"
                  class="gift-btn"
                  @click="openGiftModal(item)"
                >
                  🎁 赠送
                </button>
              </div>
            </div>
          </div>
          </div>
        </div>

        <!-- 全部商品视图 -->
        <div v-else>
          <div v-if="loading" class="loading">加载中...</div>
          <div v-else-if="items.length === 0" class="empty-inventory">
            <div class="empty-icon">🛍️</div>
            <p>商品列表为空</p>
            <p class="sub">去设置页添加商品吧~</p>
          </div>
          <div v-else class="items-container">
            <div class="items-grid">
            <div
              v-for="item in items"
              :key="item.id"
              :class="['item-card', { selected: selected?.id === item.id, disabled: student.food < item.price || item.stock <= 0 }]"
              @click="selectItem(item)"
            >
              <div v-if="getOwnedCount(item.id) > 0" class="owned-badge">🎒×{{ getOwnedCount(item.id) }}</div>
              <div class="item-icon">{{ item.icon }}</div>
              <div class="item-name">{{ item.name }}</div>
              <div class="item-price">💰 {{ item.price }}</div>
              <div class="item-stock">📦 {{ item.stock }}</div>
              <div v-if="student.food < item.price" class="insufficient">积分不足</div>
              <div v-else-if="item.stock <= 0" class="out-of-stock">已售罄</div>
            </div>
          </div>
          </div>

          <div v-if="selected && viewTab === 'all'" class="confirm-area">
            <p v-if="getOwnedCount(selected.id) > 0" class="rebuy-hint">💡 已拥有 {{ getOwnedCount(selected.id) }} 个，可继续兑换</p>
            <button class="confirm-btn" @click="confirmExchange" :disabled="submitting">
              {{ submitting ? '兑换中...' : '确认兑换' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 装备确认 -->
      <div v-else-if="step === 'equip'" class="equip-step">
        <div class="equip-preview">
          <div class="preview-pet">{{ student.petIcon || '❓' }}</div>
          <div class="preview-item">+ {{ pendingItem.icon }} {{ pendingItem.name }}</div>
        </div>
        <p class="equip-question">要把 <strong>{{ pendingItem.name }}</strong> 装备到宠物身上吗？</p>
        <div class="equip-actions">
          <button class="btn-equip" @click="doEquipAndClose" :disabled="submitting">
            ✅ 装备装饰
          </button>
          <button class="btn-skip" @click="skipEquip" :disabled="submitting">
            先不装备
          </button>
        </div>
      </div>

      <!-- 完成提示 -->
      <div v-else-if="step === 'done'" class="done-step">
        <div class="done-icon">{{ pendingItem.icon }}</div>
        <p>成功兑换 <strong>{{ pendingItem.name }}</strong>！</p>
        <p v-if="equippedDone" class="equipped-note">✅ 已装备到宠物身上</p>
        <button class="btn-close-done" @click="handleClose">完成</button>
      </div>

      <!-- 赠送选择弹窗 -->
      <div v-if="giftStep" class="gift-modal-overlay" @click.self="giftStep = null">
        <div class="gift-modal">
          <h3>🎁 赠送 {{ giftItem?.name }}</h3>
          <p class="gift-hint">选择要赠送给哪位同学</p>
          <div class="student-list">
            <div
              v-for="stu in otherStudents"
              :key="stu.id"
              :class="['student-option', { selected: giftTo === stu.id }]"
              @click="giftTo = stu.id"
            >
              <span class="stu-icon">{{ stu.petIcon || '❓' }}</span>
              <span class="stu-name">{{ stu.name }}</span>
            </div>
          </div>
          <div v-if="giftError" class="gift-error">{{ giftError }}</div>
          <div class="gift-actions">
            <button class="btn-cancel" @click="giftStep = null">取消</button>
            <button class="btn-confirm-gift" @click="doGift" :disabled="!giftTo || gifting">
              {{ gifting ? '赠送中...' : '确认赠送' }}
            </button>
          </div>
        </div>
      </div>

      <button class="close-btn" @click="handleClose">✕</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'

const props = defineProps({ student: { type: Object, required: true } })
const emit = defineEmits(['close', 'exchanged'])

const appStore = useAppStore()
const items = ref([])
const selected = ref(null)
const pendingItem = ref(null)
const loading = ref(true)
const error = ref(null)
const submitting = ref(false)
const step = ref('select') // select | equip | done
const equippedDone = ref(false)

const viewTab = ref('all') // all | mine
const equippingId = ref(null)

// 赠送相关
const giftStep = ref(null) // null | 'select'
const giftItem = ref(null)
const giftTo = ref(null)
const gifting = ref(false)
const giftError = ref(null)

// 我已兑换的商品（通过历史记录或 equippedItems 判断）
// 简化处理：从 exchangeRecords 中找该学生的兑换记录对应的商品
const myItems = computed(() => {
  if (!appStore.exchangeRecords.length) return []
  return appStore.exchangeRecords
    .filter(r => r.studentId === props.student.id)
    .map(r => {
      const item = appStore.shopItems.find(i => i.id === r.itemId)
      return item ? { ...item, exchangeTime: r.exchangeTime } : null
    })
    .filter(Boolean)
})

const isMyItem = (itemId) => {
  return myItems.value.some(i => i.id === itemId)
}

const getOwnedCount = (itemId) => {
  return myItems.value.filter(i => i.id === itemId).length
}

const isEquipped = (itemId) => {
  try {
    const ids = JSON.parse(props.student.equippedItems || '[]')
    return ids.includes(itemId)
  } catch { return false }
}

// 获取赠送来源
const getGiftFrom = (itemId) => {
  const record = appStore.exchangeRecords.find(r => 
    r.studentId === props.student.id && r.itemId === itemId && r.giftFromName
  )
  return record?.giftFromName || null
}

// 其他学生（用于赠送选择）
const otherStudents = computed(() => {
  return appStore.students.filter(s => s.id !== props.student.id)
})

// 打开赠送弹窗
const openGiftModal = (item) => {
  giftItem.value = item
  giftTo.value = null
  giftError.value = null
  giftStep.value = 'select'
}

// 执行赠送
const doGift = async () => {
  if (!giftTo.value || !giftItem.value) return
  gifting.value = true
  giftError.value = null
  try {
    // 找到该道具的兑换记录ID
    const record = appStore.exchangeRecords.find(r => 
      r.studentId === props.student.id && r.itemId === giftItem.value.id
    )
    if (!record) throw new Error('找不到兑换记录')
    
    await appStore.giftItem(props.student.id, giftTo.value, record.id)
    giftStep.value = null
    emit('exchanged')
  } catch (e) {
    giftError.value = e.response?.data?.message || e.message
  } finally {
    gifting.value = false
  }
}

// 快速装备（不需要先兑换）
const quickEquip = async (item) => {
  if (isEquipped(item.id)) return
  equippingId.value = item.id
  try {
    await appStore.equipItem(props.student.id, item.id)
    // 刷新学生数据以更新 equippedItems
    emit('exchanged')
    // 立即刷新本地学生数据
    await appStore.fetchStudents()
  } catch (e) {
    console.error('装备失败', e)
  } finally {
    equippingId.value = null
  }
}

const selectItem = (item) => {
  if (props.student.food < item.price || item.stock <= 0) return
  selected.value = item
}

const confirmExchange = async () => {
  if (!selected.value) return
  submitting.value = true
  try {
    await appStore.exchangeItem(props.student.id, selected.value.id)
    // 刷新兑换记录以便「我的道具」能显示
    await appStore.fetchExchangeRecords()
    pendingItem.value = selected.value
    step.value = 'equip'
    emit('exchanged')
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    submitting.value = false
  }
}

const doEquipAndClose = async () => {
  if (!pendingItem.value) return
  submitting.value = true
  try {
    await appStore.equipItem(props.student.id, pendingItem.value.id)
    equippedDone.value = true
    step.value = 'done'
  } catch (e) {
    error.value = e.response?.data?.message || e.message
    step.value = 'done'
  } finally {
    submitting.value = false
  }
}

const skipEquip = () => {
  step.value = 'done'
}

const handleClose = () => {
  emit('close')
}

onMounted(async () => {
  try {
    await appStore.fetchShopItems()
    await appStore.fetchExchangeRecords()
    items.value = appStore.shopItems
  } catch (e) {
    error.value = '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center;
  z-index: 100;
}

.modal {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 20px;
  padding: 25px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  position: relative;
}

.modal h2 { text-align: center; color: #92400e; margin-bottom: 12px; }

.student-info {
  display: flex; align-items: center; justify-content: center;
  gap: 10px; padding: 10px;
  background: white; border-radius: 12px; margin-bottom: 12px;
}
.student-info .pet-icon { font-size: 2rem; }
.student-info .student-name { font-weight: bold; }
.student-info .food-remaining { color: #b97323; font-weight: bold; }

.step-indicator {
  display: flex; align-items: center; justify-content: center;
  gap: 6px; margin-bottom: 10px; font-size: 13px; color: #b45309;
}
.tab-bar {
  display: flex; gap: 6px; margin-bottom: 12px; justify-content: center;
}
.tab-bar button {
  padding: 5px 14px; border: 1px solid #f59e0b; background: white;
  border-radius: 20px; cursor: pointer; font-size: 12px; color: #b45309;
  transition: all 0.2s;
}
.tab-bar button.active {
  background: #f59e0b; color: white; font-weight: bold;
}

.owned-badge {
  position: absolute; top: 4px; right: 4px; font-size: 12px;
}
.equipped-badge {
  font-size: 10px; color: #059669; font-weight: bold; margin-top: 2px;
}
.re-equip-btn {
  margin-top: 4px; padding: 3px 8px; background: #f59e0b;
  color: white; border: none; border-radius: 10px; font-size: 11px; cursor: pointer;
}
.re-equip-btn:disabled { background: #ccc; }
.gift-btn {
  margin-top: 4px; padding: 3px 8px; background: #ec4899;
  color: white; border: none; border-radius: 10px; font-size: 11px; cursor: pointer;
}
.gift-btn:hover { background: #db2777; }
.item-actions {
  display: flex; gap: 4px; justify-content: center; margin-top: 4px;
}
.gift-badge {
  font-size: 10px; color: #ec4899; font-weight: bold; margin-top: 2px;
}

.empty-inventory {
  text-align: center; padding: 30px 10px; color: #92400e;
}
.empty-icon { font-size: 3rem; margin-bottom: 10px; }
.empty-inventory p { margin: 4px 0; }
.empty-inventory .sub { color: #b45309; opacity: 0.7; font-size: 13px; }

.mine-item { position: relative; }
.mine-item .item-icon { font-size: 1.8rem; }
.mine-item .item-name { font-weight: 600; margin: 4px 0; font-size: 13px; }

.item-card { position: relative; }
.step-indicator span.active { font-weight: 700; color: #d97706; }
.step-indicator span.done { color: #059669; }
.step-indicator .arrow { color: #b45309; opacity: 0.5; }

.loading, .error { text-align: center; padding: 20px; color: #666; }
.error-banner {
  background: #fee2e2; color: #dc2626; border: 1px solid #fca5a5;
  border-radius: 10px; padding: 10px 16px; margin-bottom: 10px;
  cursor: pointer; font-size: 13px; text-align: center;
}
.error { color: #dc2626; }

/* 商品列表容器 - 带自定义滚动条 */
.items-container {
  max-height: 320px;
  overflow-y: auto;
  padding-right: 4px;
  margin: 0 -4px;
}

/* 自定义滚动条样式 */
.items-container::-webkit-scrollbar {
  width: 6px;
}
.items-container::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 10px;
}
.items-container::-webkit-scrollbar-thumb {
  background: rgba(245, 158, 11, 0.5);
  border-radius: 10px;
}
.items-container::-webkit-scrollbar-thumb:hover {
  background: rgba(245, 158, 11, 0.8);
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 10px;
  padding: 4px;
}
.item-card {
  background: white; border-radius: 12px; padding: 10px;
  text-align: center; cursor: pointer;
  border: 2px solid transparent; transition: all 0.2s;
}
.item-card:hover { border-color: #d97706; }
.item-card.selected { border-color: #059669; background: #ecfdf5; }
.item-card.disabled { opacity: 0.5; cursor: not-allowed; }
.item-icon { font-size: 1.8rem; }
.item-name { font-weight: 600; margin: 4px 0; font-size: 13px; }
.item-price { color: #dc2626; font-weight: bold; font-size: 13px; }
.item-stock { font-size: 11px; color: #666; }
.insufficient, .out-of-stock { color: #dc2626; font-size: 11px; margin-top: 4px; }

.confirm-area { margin-top: 12px; text-align: center; }
.rebuy-hint {
  font-size: 12px; color: #059669; margin-bottom: 8px;
  background: #ecfdf5; padding: 4px 10px; border-radius: 10px;
  display: inline-block;
}
.confirm-btn {
  padding: 10px 36px; background: #059669; color: white;
  border: none; border-radius: 25px; font-size: 1rem; cursor: pointer;
}
.confirm-btn:disabled { background: #9ca3af; cursor: not-allowed; }

/* 装备步骤 */
.equip-step { text-align: center; padding: 10px 0; }
.equip-preview {
  display: flex; align-items: center; justify-content: center;
  gap: 20px; margin-bottom: 16px;
}
.preview-pet { font-size: 3rem; }
.preview-item { font-size: 2rem; }
.equip-question { color: #92400e; font-size: 15px; margin-bottom: 16px; }
.equip-actions { display: flex; flex-direction: column; gap: 10px; }
.btn-equip {
  padding: 12px; background: #059669; color: white;
  border: none; border-radius: 12px; font-size: 1rem; cursor: pointer;
}
.btn-skip {
  padding: 10px; background: white; color: #b45309;
  border: 1px solid #f59e0b; border-radius: 12px; font-size: 0.9rem; cursor: pointer;
}
.btn-equip:disabled, .btn-skip:disabled { opacity: 0.6; cursor: not-allowed; }

/* 完成步骤 */
.done-step { text-align: center; padding: 10px 0; }
.done-icon { font-size: 3rem; margin-bottom: 10px; animation: bounce-in 0.5s ease; }
.done-step p { color: #92400e; font-size: 15px; }
.equipped-note { color: #059669 !important; font-size: 13px !important; margin-top: 6px !important; }
.btn-close-done {
  margin-top: 16px; padding: 10px 36px;
  background: #d97706; color: white;
  border: none; border-radius: 25px; font-size: 1rem; cursor: pointer;
}
@keyframes bounce-in {
  0% { transform: scale(0.5); opacity: 0; }
  60% { transform: scale(1.2); }
  100% { transform: scale(1); opacity: 1; }
}

.close-btn {
  position: absolute; top: 12px; right: 15px;
  background: none; border: none; font-size: 1.4rem;
  cursor: pointer; color: #92400e; opacity: 0.6;
}
.close-btn:hover { opacity: 1; }

/* 赠送弹窗 */
.gift-modal-overlay {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center;
  border-radius: 20px; z-index: 10;
}
.gift-modal {
  background: white; border-radius: 16px; padding: 20px;
  width: 90%; max-width: 350px;
}
.gift-modal h3 { text-align: center; color: #ec4899; margin-bottom: 8px; }
.gift-hint { text-align: center; color: #666; font-size: 13px; margin-bottom: 12px; }
.student-list { max-height: 220px; overflow-y: auto; }

/* 人员列表自定义滚动条 */
.student-list::-webkit-scrollbar { width: 5px; }
.student-list::-webkit-scrollbar-track { background: rgba(236, 72, 153, 0.08); border-radius: 10px; }
.student-list::-webkit-scrollbar-thumb { background: rgba(236, 72, 153, 0.4); border-radius: 10px; }
.student-list::-webkit-scrollbar-thumb:hover { background: rgba(236, 72, 153, 0.7); }
.student-option {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; border-radius: 10px; cursor: pointer;
  border: 2px solid transparent; transition: all 0.2s;
}
.student-option:hover { background: #fdf2f8; }
.student-option.selected { background: #fce7f3; border-color: #ec4899; }
.stu-icon { font-size: 1.5rem; }
.stu-name { font-weight: 600; color: #374151; }
.gift-error {
  background: #fee2e2; color: #dc2626; padding: 8px; border-radius: 8px;
  font-size: 12px; margin-top: 10px; text-align: center;
}
.gift-actions {
  display: flex; gap: 10px; margin-top: 16px; justify-content: center;
}
.btn-cancel {
  padding: 8px 20px; background: #e5e7eb; color: #374151;
  border: none; border-radius: 10px; cursor: pointer;
}
.btn-confirm-gift {
  padding: 8px 20px; background: #ec4899; color: white;
  border: none; border-radius: 10px; cursor: pointer;
}
.btn-confirm-gift:disabled { background: #ccc; cursor: not-allowed; }
</style>
