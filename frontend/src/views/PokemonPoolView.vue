<template>
  <div class="pokemon-pool-page">
    <div class="container">
      <!-- 导航栏 -->
      <div class="navbar">
        <div class="navbar-left">
          <button class="back-btn" @click="router.push('/home')">← 返回首页</button>
        </div>
        <div class="navbar-center">
          <h1>🎮 宝可梦池管理</h1>
        </div>
        <div class="navbar-right">
          <span class="pool-count">已选择: {{ selectedEntries.length }} 只</span>
          <button class="clear-btn" :disabled="saving || selectedEntries.length === 0" @click="clearAll">
            🗑 清空
          </button>
          <button class="ball-btn" :disabled="distributing" @click="distributeBalls">
            {{ distributing ? '发放中...' : '🔮 发放精灵球' }}
          </button>
          <button class="save-btn" :disabled="saving || selectedEntries.length === 0" @click="savePool">
            {{ saving ? '保存中...' : '💾 保存设置' }}
          </button>
        </div>
      </div>

      <!-- 说明 -->
      <div class="info-banner">
        <span>ℹ️ 点击宝可梦选中，点击"+"调整抽取概率权重（权重越高越容易被抽到）。拖动滑块可批量调整权重。</span>
      </div>

      <!-- 批量权重控制 -->
      <div v-if="selectedEntries.length > 0" class="batch-weight-bar">
        <span class="batch-label">批量权重:</span>
        <input type="range" min="1" max="10" step="1" v-model.number="batchWeight" class="weight-slider" />
        <span class="batch-value">{{ batchWeight }}</span>
        <button class="batch-apply-btn" @click="applyBatchWeight">应用</button>
        <span class="pool-info">总权重: {{ totalWeight.toFixed(1) }}</span>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>

      <!-- 宝可梦网格 -->
      <div v-else class="pokemon-grid">
        <div
          v-for="pokemon in allPokemon"
          :key="pokemon.pokedexId"
          :class="['pokemon-card', { selected: getEntry(pokemon.pokedexId) }]"
          @click="togglePokemon(pokemon)"
        >
          <div class="pokemon-image">
            <img :src="pokemon.image" :alt="pokemon.name" @error="handleImageError">
          </div>
          <div class="pokemon-info">
            <span class="pokedex-number">#{{ pokemon.pokedexId }}</span>
            <span class="pokemon-name">{{ pokemon.name }}</span>
            <div class="pokemon-types">
              <span v-for="type in pokemon.types" :key="type" :class="['type-badge', `type-${type}`]">
                {{ type }}
              </span>
            </div>
          </div>
          <!-- 权重控制 -->
          <div v-if="getEntry(pokemon.pokedexId)" class="weight-control" @click.stop>
            <button class="weight-dec" @click="decWeight(pokemon.pokedexId)">−</button>
            <input
              type="number"
              class="weight-input"
              :value="getWeight(pokemon.pokedexId)"
              min="1"
              max="10"
              step="0.5"
              @change="setWeight(pokemon.pokedexId, $event)"
            >
            <button class="weight-inc" @click="incWeight(pokemon.pokedexId)">+</button>
          </div>
          <div v-if="getEntry(pokemon.pokedexId)" class="selected-check">✓</div>
        </div>
      </div>

      <!-- 保存成功提示 -->
      <div v-if="saveSuccess" class="toast success">✅ 保存成功！</div>
      <div v-if="saveError" class="toast error">❌ 保存失败: {{ saveError }}</div>
      <div v-if="clearSuccess" class="toast success">✅ 已清空宝可梦池！</div>
      <div v-if="distSuccess" class="toast success">✅ 精灵球发放成功！</div>
      <div v-if="distError" class="toast error">❌ 发放失败: {{ distError }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { petApi, pokemonApi } from '../api/index.js'

const router = useRouter()
const authStore = useAuthStore()

const allPokemon = ref([])
const selectedEntries = ref([]) // [{pokedexId, weight}]
const loading = ref(true)
const saving = ref(false)
const saveSuccess = ref(false)
const saveError = ref(false)
const clearSuccess = ref(false)
const distributing = ref(false)
const distSuccess = ref(false)
const distError = ref(false)
const batchWeight = ref(3)

onMounted(async () => { await loadData() })

async function loadData() {
  loading.value = true
  try {
    const pokemonRes = await petApi.getPokemon()
    allPokemon.value = pokemonRes.data

    const teacherId = authStore.teacherId
    if (teacherId) {
      const poolRes = await pokemonApi.getClassroomPool(teacherId)
      const pool = poolRes.data
      if (pool && pool.pokedexEntries) {
        const entries = typeof pool.pokedexEntries === 'string'
          ? JSON.parse(pool.pokedexEntries)
          : pool.pokedexEntries
        selectedEntries.value = entries.map(e => ({
          pokedexId: Number(e.pokedexId),
          weight: parseFloat(e.weight || 1.0)
        }))
      }
    }
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

function getEntry(pokedexId) {
  return selectedEntries.value.some(e => e.pokedexId === pokedexId)
}

function getWeight(pokedexId) {
  const entry = selectedEntries.value.find(e => e.pokedexId === pokedexId)
  return entry ? entry.weight : 1.0
}

function togglePokemon(pokemon) {
  const idx = selectedEntries.value.findIndex(e => e.pokedexId === pokemon.pokedexId)
  if (idx >= 0) {
    selectedEntries.value.splice(idx, 1)
  } else {
    selectedEntries.value.push({ pokedexId: pokemon.pokedexId, weight: batchWeight.value })
  }
}

function setWeight(pokedexId, event) {
  const val = parseFloat(event.target.value)
  if (!isNaN(val) && val >= 0.5 && val <= 10) {
    const entry = selectedEntries.value.find(e => e.pokedexId === pokedexId)
    if (entry) entry.weight = val
  }
}

function incWeight(pokedexId) {
  const entry = selectedEntries.value.find(e => e.pokedexId === pokedexId)
  if (entry && entry.weight < 10) { entry.weight = Math.min(10, entry.weight + 0.5) }
  else if (!entry) { selectedEntries.value.push({ pokedexId, weight: 1.5 }) }
}

function decWeight(pokedexId) {
  const entry = selectedEntries.value.find(e => e.pokedexId === pokedexId)
  if (entry && entry.weight > 0.5) { entry.weight = Math.max(0.5, entry.weight - 0.5) }
}

function applyBatchWeight() {
  selectedEntries.value.forEach(e => { e.weight = batchWeight.value })
}

const totalWeight = computed(() =>
  selectedEntries.value.reduce((sum, e) => sum + e.weight, 0)
)

async function savePool() {
  saving.value = true
  saveSuccess.value = false
  saveError.value = false
  try {
    const teacherId = authStore.teacherId
    const entries = selectedEntries.value.map(e => ({
      pokedexId: e.pokedexId,
      weight: parseFloat(e.weight.toFixed(1))
    }))
    await pokemonApi.setClassroomPool({
      classroomId: teacherId,
      pokedexEntries: entries
    })
    saveSuccess.value = true
    setTimeout(() => { saveSuccess.value = false }, 3000)
  } catch (e) {
    saveError.value = e.response?.data?.error || '保存失败'
    setTimeout(() => { saveError.value = false }, 5000)
  } finally {
    saving.value = false
  }
}

async function clearAll() {
  selectedEntries.value = []
  clearSuccess.value = true
  setTimeout(() => { clearSuccess.value = false }, 3000)
  await savePool()
}

async function distributeBalls() {
  distributing.value = true
  distSuccess.value = false
  distError.value = false
  try {
    await pokemonApi.distributeBalls()
    distSuccess.value = true
    setTimeout(() => { distSuccess.value = false }, 3000)
  } catch (e) {
    distError.value = e.response?.data?.error || '发放失败'
    setTimeout(() => { distError.value = false }, 5000)
  } finally {
    distributing.value = false
  }
}

function handleImageError(e) {
  e.target.src = '/pokemon/placeholder.png'
}
</script>

<style scoped>
.pokemon-pool-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.2);
}

.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 2px solid #eee;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.navbar-center h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.pool-count {
  font-size: 14px;
  color: #666;
  background: #f5f5f5;
  padding: 8px 16px;
  border-radius: 20px;
}

.save-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.save-btn:disabled, .clear-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.clear-btn {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
}

.clear-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

.ball-btn {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
}

.ball-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
}

.back-btn {
  background: #f5f5f5;
  color: #333;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.back-btn:hover { background: #e0e0e0; }

.info-banner {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #555;
}

.batch-weight-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8faff;
  border: 1px solid #e0e8ff;
  padding: 10px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.batch-label {
  font-size: 14px;
  font-weight: 600;
  color: #555;
}

.weight-slider {
  width: 120px;
  accent-color: #667eea;
}

.batch-value {
  font-size: 16px;
  font-weight: 700;
  color: #667eea;
  min-width: 20px;
}

.batch-apply-btn {
  background: #667eea;
  color: white;
  border: none;
  padding: 5px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}

.pool-info {
  font-size: 13px;
  color: #888;
  margin-left: auto;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  color: #666;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

.pokemon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 14px;
}

.pokemon-card {
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.pokemon-card:hover { transform: translateY(-4px); box-shadow: 0 6px 20px rgba(0,0,0,0.1); }

.pokemon-card.selected {
  border-color: #667eea;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
}

.pokemon-image {
  width: 100%;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.pokemon-image img {
  width: 80px;
  height: 80px;
  object-fit: contain;
}

.pokemon-info { text-align: center; }

.pokedex-number { font-size: 11px; color: #999; }

.pokemon-name {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin: 4px 0;
}

.pokemon-types {
  display: flex;
  justify-content: center;
  gap: 4px;
  flex-wrap: wrap;
}

.type-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  color: white;
  font-weight: 500;
}

.type-normal { background: #A8A878; }
.type-fire { background: #F08030; }
.type-water { background: #6890F0; }
.type-electric { background: #F8D030; }
.type-grass { background: #78C850; }
.type-ice { background: #98D8D8; }
.type-fighting { background: #C03028; }
.type-poison { background: #A040A0; }
.type-ground { background: #E0C068; }
.type-flying { background: #A890F0; }
.type-psychic { background: #F85888; }
.type-bug { background: #A8B820; }
.type-rock { background: #B8A038; }
.type-ghost { background: #705898; }
.type-dragon { background: #7038F8; }
.type-dark { background: #705848; }
.type-steel { background: #B8B8D0; }
.type-fairy { background: #EE99AC; }

.selected-check {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  background: #667eea;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: bold;
}

/* 权重控制 */
.weight-control {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #eee;
}

.weight-dec, .weight-inc {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px solid #ddd;
  background: #f5f5f5;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  color: #555;
  flex-shrink: 0;
}

.weight-dec:hover, .weight-inc:hover { background: #667eea; color: white; border-color: #667eea; }

.weight-input {
  width: 40px;
  text-align: center;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 2px 4px;
  font-size: 13px;
  font-weight: 600;
  color: #667eea;
}

.toast {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  animation: slideUp 0.3s ease;
  z-index: 1000;
}

.toast.success { background: #10b981; color: white; }
.toast.error { background: #ef4444; color: white; }

@keyframes slideUp {
  from { opacity: 0; transform: translateX(-50%) translateY(20px); }
  to { opacity: 1; transform: translateX(-50%) translateY(0); }
}
</style>
