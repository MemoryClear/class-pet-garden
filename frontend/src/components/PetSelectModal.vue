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
      
      <!-- Tab 切换 -->
      <div class="pet-tabs">
        <button 
          class="tab-btn" 
          :class="{ active: activeTab === 'normal' }"
          @click="switchTab('normal')"
        >
          🐾 普通宠物
        </button>
        <button 
          class="tab-btn pokemon-tab" 
          :class="{ active: activeTab === 'pokemon' }"
          @click="switchTab('pokemon')"
        >
          🎮 宝可梦
        </button>
      </div>
      
      <!-- 普通宠物列表 -->
      <div class="pet-grid" v-show="activeTab === 'normal'">
        <div v-if="appStore.petLibrary.length === 0" class="empty-tip">
          加载中...
        </div>
        <div v-for="pet in appStore.petLibrary" :key="pet.id"
             class="pet-option"
             :class="{ selected: selected?.id === pet.id && selected?.type === 'normal' }"
             @click="selectPet(pet, 'normal')">
          <div class="pet-icon">{{ pet.icon }}</div>
          <div class="pet-name">{{ pet.name }}</div>
        </div>
      </div>
      
      <!-- 宝可梦列表 -->
      <div class="pet-grid pokemon-grid" v-show="activeTab === 'pokemon'">
        <div v-if="pokemonList.length === 0" class="empty-tip">
          {{ pokemonLoading ? '加载中...' : '暂无宝可梦' }}
        </div>
        <div v-for="pokemon in pokemonList" :key="pokemon.id"
             class="pet-option pokemon-option"
             :class="{ selected: selected?.id === pokemon.id && selected?.type === 'pokemon' }"
             @click="selectPet(pokemon, 'pokemon')">
          <div class="pet-icon pokemon-icon">
            <img :src="pokemon.image" :alt="pokemon.name" v-if="pokemon.image" class="pokemon-img" />
            <span v-else>{{ pokemon.icon }}</span>
          </div>
          <div class="pet-name">{{ pokemon.name }}</div>
          <div class="pokemon-types" v-if="pokemon.types">
            <span v-for="t in pokemon.types" :key="t" class="type-tag" :class="getTypeClass(t)">{{ t }}</span>
          </div>
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
import { ref, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'
import { petApi } from '../api/index.js'
import $confirm from '../composables/useConfirmModal.js'

const props = defineProps({ student: { type: Object, required: true } })
const emit = defineEmits(['close'])
const appStore = useAppStore()

const activeTab = ref('normal')
const selected = ref(null)
const loading = ref(false)
const pokemonList = ref([])
const pokemonLoading = ref(false)

onMounted(async () => {
  // 加载普通宠物
  if (!appStore.petLibrary.length) {
    await appStore.fetchPetLibrary()
  }
})

async function switchTab(tab) {
  activeTab.value = tab
  // 切换到宝可梦 tab 时加载数据
  if (tab === 'pokemon' && pokemonList.value.length === 0) {
    await loadPokemon()
  }
}

async function loadPokemon() {
  pokemonLoading.value = true
  try {
    const { data } = await petApi.getPokemon()
    pokemonList.value = data || []
  } catch (e) {
    console.error('加载宝可梦失败', e)
    $confirm.error('加载宝可梦失败')
  } finally {
    pokemonLoading.value = false
  }
}

function selectPet(pet, type) {
  selected.value = { ...pet, type }
}

function getTypeClass(type) {
  const typeColors = {
    '草': 'type-grass',
    '火': 'type-fire',
    '水': 'type-water',
    '电': 'type-electric',
    '一般': 'type-normal',
    '飞行': 'type-flying',
    '毒': 'type-poison',
    '虫': 'type-bug',
    '妖精': 'type-fairy',
    '超能力': 'type-psychic',
    '格斗': 'type-fighting',
    '岩石': 'type-rock',
    '地面': 'type-ground',
    '幽灵': 'type-ghost',
    '龙': 'type-dragon',
    '恶': 'type-dark',
    '钢': 'type-steel',
    '冰': 'type-ice'
  }
  return typeColors[type] || ''
}

async function confirm() {
  if (!selected.value || loading.value) return
  
  // 检查更换卡
  if (props.student.petId && (props.student.petChangeCards ?? 0) <= 0) {
    $confirm.alert('请先到小卖部购买宠物更换卡', { type: 'warn', title: '提示' })
    return
  }
  
  loading.value = true
  try {
    // 宝可梦用 image 作为 icon，普通宠物用 emoji
    const petIcon = selected.value.image || selected.value.icon
    await appStore.adoptPet(props.student.id, selected.value.id, selected.value.name, petIcon)
    emit('close')
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '操作失败'
    $confirm.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 999; padding: 20px; }
.modal-card { background: #fff; border-radius: 20px; width: 100%; max-width: 520px; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 20px; border-bottom: 1px solid #f0f0f0; }
.modal-title { font-size: 18px; font-weight: 600; }
.modal-close { background: #f0f0f0; border: none; width: 32px; height: 32px; border-radius: 50%; font-size: 20px; cursor: pointer; display: flex; align-items: center; justify-content: center; }

/* Tab 样式 */
.pet-tabs { display: flex; gap: 8px; padding: 12px 20px; border-bottom: 1px solid #f0f0f0; }
.tab-btn { 
  flex: 1; 
  padding: 10px 16px; 
  border: none; 
  border-radius: 10px; 
  background: #f5f5f5; 
  font-size: 14px; 
  font-weight: 500; 
  cursor: pointer; 
  transition: all 0.2s; 
}
.tab-btn:hover { background: #e8e8e8; }
.tab-btn.active { 
  background: linear-gradient(135deg, #ff6b9d 0%, #ff4777 100%); 
  color: white; 
}
.tab-btn.pokemon-tab.active { 
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); 
}

/* 宠物网格 */
.pet-grid { padding: 16px; display: grid; grid-template-columns: repeat(auto-fill, minmax(90px, 1fr)); gap: 12px; max-height: 45vh; overflow-y: auto; }
.pokemon-grid { grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); }

.pet-option { background: #f7fafc; border-radius: 12px; padding: 14px 8px; text-align: center; cursor: pointer; border: 2px solid transparent; transition: all 0.2s; }
.pet-option:hover { background: #f0f4f8; }
.pet-option.selected { border-color: #ff4d79; background: #fff0f3; }
.pet-option.pokemon-option.selected { border-color: #4facfe; background: #e6f7ff; }

.pet-icon { font-size: 40px; margin-bottom: 4px; }
.pokemon-icon { font-size: 36px; display: flex; align-items: center; justify-content: center; width: 60px; height: 60px; }
.pokemon-img { width: 100%; height: 100%; object-fit: contain; }
.pet-name { font-size: 13px; color: #2d3748; font-weight: 500; }

/* 宝可梦属性标签 */
.pokemon-types { display: flex; justify-content: center; gap: 4px; margin-top: 4px; flex-wrap: wrap; }
.type-tag { 
  font-size: 10px; 
  padding: 2px 6px; 
  border-radius: 4px; 
  color: white; 
  font-weight: 500;
}
.type-grass { background: #78c850; }
.type-fire { background: #f08030; }
.type-water { background: #6890f0; }
.type-electric { background: #f8d030; color: #333; }
.type-normal { background: #a8a878; }
.type-flying { background: #a890f0; }
.type-poison { background: #a040a0; }
.type-bug { background: #a8b820; }
.type-fairy { background: #ee99ac; color: #333; }
.type-psychic { background: #f85888; }
.type-fighting { background: #c03028; }
.type-rock { background: #b8a038; }
.type-ground { background: #e0c068; color: #333; }
.type-ghost { background: #705898; }
.type-dragon { background: #7038f8; }
.type-dark { background: #705848; }
.type-steel { background: #b8b8d0; color: #333; }
.type-ice { background: #98d8d8; color: #333; }

.empty-tip { grid-column: 1/-1; text-align: center; padding: 20px; color: #999; font-size: 14px; }

.modal-footer { display: flex; gap: 12px; padding: 16px 20px; border-top: 1px solid #f0f0f0; }
.btn-cancel { flex: 1; padding: 12px; background: #f0f0f0; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; }
.btn-confirm { flex: 1; padding: 12px; background: linear-gradient(90deg, #f04e98 0%, #ed266e 100%); color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 500; }
.btn-confirm:disabled { opacity: 0.5; cursor: not-allowed; }

.card-warning { padding: 12px 20px; background: #fef3c7; }
.card-warning .has-card { font-size: 13px; color: #92400e; }
.card-warning .no-card { font-size: 13px; color: #dc2626; font-weight: 500; }
</style>
