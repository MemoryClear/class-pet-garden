<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-card">
      <div class="modal-header">
        <div class="modal-title">🎁 选一个宠物</div>
        <button class="modal-close" @click="emit('close')">×</button>
      </div>
      
      <!-- 更换卡提示 -->
      <div v-if="props.student.petId" class="card-warning">
        <div v-if="(props.student.petChangeCards ?? 0) > 0" class="has-card">
          🎫 更换卡 ×{{ props.student.petChangeCards }}，换宠物将消耗 1 张
        </div>
        <div v-else class="no-card">
          ⚠️ 您没有宠物更换卡，请先到小卖部购买 🎫宠物更换卡
        </div>
      </div>
      
      <div class="pet-grid">
        <div v-if="appStore.petLibrary.length === 0" class="empty-tip">
          加载中...
        </div>
        <div v-for="pet in appStore.petLibrary" :key="pet.id"
             class="pet-option"
             :class="{ selected: selected === pet.id }"
             @click="selected = pet.id">
          <div class="pet-icon">{{ pet.icon }}</div>
          <div class="pet-name">{{ pet.name }}</div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="emit('close')">取消</button>
        <button 
          class="btn-confirm" 
          @click="confirm" 
          :disabled="!selected || (props.student.petId && (props.student.petChangeCards ?? 0) <= 0)"
        >
          {{ loading ? '处理中...' : props.student.petId ? '确认更换' : '确认领养' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAppStore } from '../stores/app.js'

const props = defineProps({ student: { type: Object, required: true } })
const emit = defineEmits(['close'])
const appStore = useAppStore()
const selected = ref(props.student.petId || null)
const loading = ref(false)

async function confirm() {
  if (!selected.value || loading.value) return
  
  // 检查更换卡
  if (props.student.petId && (props.student.petChangeCards ?? 0) <= 0) {
    alert('请先到小卖部购买宠物更换卡')
    return
  }
  
  loading.value = true
  try {
    await appStore.adoptPet(props.student.id, selected.value)
    emit('close')
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '操作失败'
    alert(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 999; padding: 20px; }
.modal-card { background: #fff; border-radius: 20px; width: 100%; max-width: 480px; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 20px; border-bottom: 1px solid #f0f0f0; }
.modal-title { font-size: 18px; font-weight: 600; }
.modal-close { background: #f0f0f0; border: none; width: 32px; height: 32px; border-radius: 50%; font-size: 20px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.pet-grid { padding: 16px; display: grid; grid-template-columns: repeat(auto-fill, minmax(90px, 1fr)); gap: 12px; max-height: 50vh; overflow-y: auto; }
.pet-option { background: #f7fafc; border-radius: 12px; padding: 14px 8px; text-align: center; cursor: pointer; border: 2px solid transparent; transition: all 0.2s; }
.pet-option.selected { border-color: #ff4d79; background: #fff0f3; }
.pet-icon { font-size: 40px; margin-bottom: 4px; }
.pet-name { font-size: 13px; color: #2d3748; font-weight: 500; }
.empty-tip { grid-column: 1/-1; text-align: center; padding: 20px; color: #999; font-size: 14px; }
.modal-footer { display: flex; gap: 12px; padding: 16px 20px; border-top: 1px solid #f0f0f0; }
.btn-cancel { flex: 1; padding: 12px; background: #f0f0f0; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; }
.btn-confirm { flex: 1; padding: 12px; background: linear-gradient(90deg, #f04e98 0%, #ed266e 100%); color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 500; }
.btn-confirm:disabled { opacity: 0.5; cursor: not-allowed; }

.card-warning { padding: 12px 20px; background: #fef3c7; }
.card-warning .has-card { font-size: 13px; color: #92400e; }
.card-warning .no-card { font-size: 13px; color: #dc2626; font-weight: 500; }
</style>