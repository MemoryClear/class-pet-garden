<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content pokemon-modal">
      <div class="modal-header">
        <h3>🎮 {{ student?.name }} 的宝可梦图鉴</h3>
        <button class="close-btn" @click="$emit('close')">✕</button>
      </div>

      <div class="modal-body">
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="error" class="error">{{ error }}</div>
        <template v-else>
          <div class="info-bar">
            <span>🏆 共 {{ pokemonList.length }} 只宝可梦</span>
            <span>🎫 宠物更换卡: {{ petChangeCards }}</span>
            <span v-if="representName" class="represent-badge">⭐ 代表: {{ representName }}</span>
          </div>

          <div class="pokemon-grid" v-if="pokemonList.length > 0">
            <div
              v-for="poke in pokemonList"
              :key="poke.id"
              class="pokemon-item"
              :class="{ 'is-represent': poke.id === representPokemonId }"
              @click="handleSelect(poke)"
            >
              <div class="poke-img-wrap">
                <img :src="poke.image || '/pokemon/default.png'" :alt="poke.name" class="poke-img" />
                <div v-if="poke.id === representPokemonId" class="crown">⭐</div>
              </div>
              <div class="poke-info">
                <div class="poke-name">{{ poke.name }}</div>
                <div class="poke-level">Lv.{{ poke.level }}</div>
                <div class="poke-food">🍖 {{ poke.food }}</div>
              </div>
            </div>
          </div>

          <div v-else class="empty">
            <p>📭 这位同学还没有宝可梦</p>
            <p class="hint">使用精灵球可以捕捉宝可梦哦</p>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { studentApi } from '../api/index.js'
import $confirm from '../composables/useConfirmModal.js'

const props = defineProps({
  student: { type: Object, required: true },
  visible: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'updated'])

const loading = ref(false)
const error = ref('')
const pokemonList = ref([])
const representPokemonId = ref(null)
const petChangeCards = ref(0)

const representName = computed(() => {
  if (!representPokemonId.value) return null
  const poke = pokemonList.value.find(p => p.id === representPokemonId.value)
  return poke ? poke.name : null
})

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const res = await studentApi.getStudentPokemon(props.student.id)
    pokemonList.value = (res.data.pokemonList || []).filter(p => p && p.name)
    representPokemonId.value = res.data.representPokemonId || null
    petChangeCards.value = res.data.petChangeCards || 0
  } catch (e) {
    error.value = e.response?.data?.error || '加载失败'
  } finally {
    loading.value = false
  }
}

async function handleSelect(poke) {
  if (poke.id === representPokemonId.value) {
    $confirm.alert('这只已经是代表宝可梦了')
    return
  }
  const ok = await $confirm.confirm(
    `确定将 ${poke.name} 设为代表宝可梦吗？`,
    { message: '将消耗 1 张宠物更换卡' }
  )
  if (!ok) return

  try {
    await studentApi.setRepresentPokemon(props.student.id, poke.id)
    representPokemonId.value = poke.id
    petChangeCards.value--
    emit('updated')
    $confirm.success(`${poke.name} 已设为代表宝可梦`)
  } catch (e) {
    $confirm.error(e.response?.data?.error || '设置失败')
  }
}



watch(() => props.visible, (v) => { if (v) loadData() })
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-content {
  background: #fff; border-radius: 16px; width: 90%; max-width: 720px;
  max-height: 80vh; display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.modal-header {
  padding: 16px 20px; background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; display: flex; align-items: center; justify-content: space-between;
}
.modal-header h3 { margin: 0; font-size: 18px; }
.close-btn {
  background: rgba(255,255,255,0.2); border: none; color: #fff;
  width: 32px; height: 32px; border-radius: 50%; cursor: pointer; font-size: 16px;
  display: flex; align-items: center; justify-content: center;
}
.close-btn:hover { background: rgba(255,255,255,0.3); }
.modal-body {
  padding: 16px; overflow-y: auto; flex: 1;
}
.loading, .error { text-align: center; padding: 40px; color: #666; }
.error { color: #ef4444; }
.info-bar {
  display: flex; gap: 12px; align-items: center; margin-bottom: 16px;
  font-size: 14px; color: #555; flex-wrap: wrap;
}
.represent-badge {
  background: #fef3c7; color: #92400e; padding: 2px 10px; border-radius: 12px;
  font-weight: 600;
}
.pokemon-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}
.pokemon-item {
  background: #f8fafc; border: 2px solid #e2e8f0; border-radius: 12px;
  padding: 10px; cursor: pointer; transition: all 0.2s; text-align: center;
}
.pokemon-item:hover { border-color: #667eea; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.pokemon-item.is-represent { border-color: #f59e0b; background: #fffbeb; }
.poke-img-wrap { position: relative; width: 80px; height: 80px; margin: 0 auto 6px; }
.poke-img { width: 100%; height: 100%; object-fit: contain; }
.crown {
  position: absolute; top: -6px; right: -6px; font-size: 18px;
  background: #fbbf24; width: 24px; height: 24px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
}
.poke-name { font-size: 13px; font-weight: 600; color: #1e293b; margin-bottom: 2px; }
.poke-level { font-size: 12px; color: #667eea; }
.poke-food { font-size: 11px; color: #94a3b8; }
.empty { text-align: center; padding: 40px; color: #94a3b8; }
.empty .hint { font-size: 13px; margin-top: 8px; }
</style>
