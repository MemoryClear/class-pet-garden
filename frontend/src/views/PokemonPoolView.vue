<template>
  <AppShell title="宝可梦池" :subtitle="subtitle">
    <template #actions>
      <span class="pool-count">已选 {{ selectedEntries.length }} 只</span>
      <button class="btn-ghost pool-btn" :disabled="saving || selectedEntries.length === 0" @click="clearAll">
        🗑<span class="btn-txt"> 清空</span>
      </button>
      <button class="btn-ghost pool-btn" :disabled="distributing" @click="distributeBalls">
        🔮<span class="btn-txt"> {{ distributing ? '发放中…' : '发放精灵球' }}</span>
      </button>
      <button class="btn-primary" :disabled="saving || selectedEntries.length === 0" @click="savePool()">
        {{ saving ? '保存中…' : '保存设置' }}
      </button>
    </template>

    <div class="page-wrap">
      <div class="container">

      <!-- 说明 -->
      <div class="info-banner">
        <span>ℹ️ 点击宝可梦选中，点击"+"调整抽取概率权重（权重越高越容易被抽到）。拖动滑块可批量调整权重。</span>
        <span class="banner-sub">🏫 池子按班级保存，左上角切换班级会切换到那个班的池子。</span>
        <span class="banner-sub">🔮 每个精灵球在<b>发放时</b>就锁定了当时的池子，之后改池子不会影响已经发出去的球。</span>
      </div>

      <!-- 快捷操作 -->
      <div class="quick-actions">
        <button class="btn-ghost" :disabled="randomBusy || !allPokemon.length" @click="randomSelect">
          🎲<span class="btn-txt"> 一键随机选择</span>
        </button>
        <button
          class="btn-ghost qa-accent"
          :disabled="randomBusy || distributing || !allPokemon.length"
          @click="randomSelectAndDistribute"
        >
          🔮<span class="btn-txt"> 一键随机发放精灵球</span>
        </button>
        <span class="qa-hint">
          随机选择只改名单，还要点「保存设置」才生效；随机发放会同时保存新池子并给全班各发 1 个精灵球。
        </span>
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
          <div class="pokemon-image" :data-pokedex-id="pokemon.pokedexId" :ref="el => registerCard(el, pokemon.pokedexId)">
            <img
              :src="isVisible(pokemon.pokedexId) ? pokemon.image : placeholderSrc"
              :alt="pokemon.name"
              :data-pokedex-id="pokemon.pokedexId"
              :class="['pokemon-sprite', { 'pokemon-sprite--loaded': isVisible(pokemon.pokedexId) }]"
              loading="lazy"
              decoding="async"
              @error="handleImageError"
            >
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
  </AppShell>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import AppShell from '../components/layout/AppShell.vue'
import { useAppStore } from '../stores/app.js'
import { petApi, pokemonApi } from '../api/index.js'
import { toast } from '../composables/useToast.js'
import $confirm from '../composables/useConfirmModal.js'

const subtitle = computed(() => {
  const cls = appStore.className || '当前班级'
  const n = selectedEntries.value.length
  return n
    ? `${cls} · 已选择 ${n} 只 · 权重越高越容易被抽到`
    : `${cls} · 点击宝可梦加入抽取池，再点「保存设置」生效`
})
const appStore = useAppStore()

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
const randomBusy = ref(false)

// —— 图片懒加载 + 数据缓存 ——
const placeholderSrc =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><rect width="80" height="80" fill="#f1f5f9"/></svg>'
  )
const visiblePokedexIds = ref(new Set())
const cardRefs = new Map()
let cardObserver = null
let scrollContainer = null
let scrollHandler = null

function isVisible(pokedexId) {
  return visiblePokedexIds.value.has(pokedexId)
}

function registerCard(el, pokedexId) {
  if (!el) {
    cardRefs.delete(pokedexId)
    return
  }
  cardRefs.set(pokedexId, el)
  if (cardObserver) {
    cardObserver.observe(el)
  }
}

function setupIntersectionObserver() {
  if (typeof IntersectionObserver === 'undefined') {
    // 兜底：全部可见
    allPokemon.value.forEach(p => visiblePokedexIds.value.add(p.pokedexId))
    return
  }
  // 视口 + 上下各 200px 缓冲区，提前加载接近屏幕的图片
  cardObserver = new IntersectionObserver(
    entries => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          const id = Number(entry.target.dataset.pokedexId)
          if (!isNaN(id)) visiblePokedexIds.value.add(id)
        }
      }
    },
    { rootMargin: '200px 0px', threshold: 0.01 }
  )
  cardRefs.forEach(el => cardObserver.observe(el))
}

function scrollToTop() {
  // 离开页面时滚回顶部，避免下次进入时残留滚动位置
  if (scrollContainer) scrollContainer.scrollTop = 0
  window.scrollTo(0, 0)
}

// 切换班级：池子跟着换
watch(() => appStore.currentClassId, (id) => { if (id) loadPool() })

onMounted(async () => {
  await loadData()
  // 数据加载完后，DOM 也已经渲染，再挂 IntersectionObserver
  await nextTick()
  scrollContainer = document.querySelector('.pokemon-grid')?.parentElement || null
  if (scrollContainer) {
    scrollHandler = () => { /* 触发 IntersectionObserver 重计算 */ }
    scrollContainer.addEventListener('scroll', scrollHandler, { passive: true })
  }
  setupIntersectionObserver()
})

onBeforeUnmount(() => {
  if (cardObserver) {
    cardObserver.disconnect()
    cardObserver = null
  }
  if (scrollContainer && scrollHandler) {
    scrollContainer.removeEventListener('scroll', scrollHandler)
  }
  scrollToTop()
})

/** 读取当前班级的池子（多班级后池子按 classId 存） */
async function loadPool() {
  const classId = appStore.currentClassId
  if (!classId) {
    selectedEntries.value = []
    return
  }
  try {
    const poolRes = await pokemonApi.getClassroomPool(classId)
    const pool = poolRes.data
    const raw = pool && pool.pokedexEntries
    const entries = typeof raw === 'string' ? JSON.parse(raw || '[]') : (raw || [])
    selectedEntries.value = entries.map(e => ({
      pokedexId: Number(e.pokedexId),
      weight: parseFloat(e.weight || 1.0)
    }))
  } catch (e) {
    console.error('读取宝可梦池失败:', e)
    selectedEntries.value = []
  }
}

async function loadData() {
  loading.value = true
  try {
    // 优先使用 Pinia 中已缓存的宝可梦库（避免重复请求 + 重复拉图片 URL 列表）
    if (appStore.pokemonLibrary && appStore.pokemonLibrary.length) {
      allPokemon.value = appStore.pokemonLibrary
    } else {
      const pokemonRes = await petApi.getPokemon()
      allPokemon.value = pokemonRes.data || []
      appStore.pokemonLibrary = allPokemon.value
      appStore.pokemonLibraryLoadedAt = Date.now()
    }

    await loadPool()
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

async function savePool({ quiet = false } = {}) {
  saving.value = true
  saveSuccess.value = false
  saveError.value = false
  try {
    const classId = appStore.currentClassId
    if (!classId) {
      saveError.value = '还没有选中班级'
      setTimeout(() => { saveError.value = false }, 4000)
      return false
    }
    const entries = selectedEntries.value.map(e => ({
      pokedexId: e.pokedexId,
      weight: parseFloat(e.weight.toFixed(1))
    }))
    await pokemonApi.setClassroomPool({
      classroomId: classId,
      pokedexEntries: entries
    })
    if (!quiet) {
      saveSuccess.value = true
      setTimeout(() => { saveSuccess.value = false }, 3000)
    }
    return true
  } catch (e) {
    saveError.value = e.response?.data?.error || '保存失败'
    setTimeout(() => { saveError.value = false }, 5000)
    return false
  } finally {
    saving.value = false
  }
}

/* ---------------- 一键随机 ---------------- */

/** 从宝可梦库里随机抽 count 只（不重复），权重取当前批量权重 */
function pickRandomEntries(count) {
  const pool = allPokemon.value.slice()
  for (let i = 0; i < count; i++) {
    const j = i + Math.floor(Math.random() * (pool.length - i))
    const t = pool[i]
    pool[i] = pool[j]
    pool[j] = t
  }
  return pool.slice(0, count).map(p => ({
    pokedexId: Number(p.pokedexId),
    weight: batchWeight.value
  }))
}

/** 问用户要抽几只，取消返回 null */
async function askRandomCount(title, okText) {
  const max = allPokemon.value.length
  if (!max) {
    $confirm.error('宝可梦库还没加载好，请稍后再试')
    return null
  }
  const def = Math.min(6, max)
  const val = await $confirm.prompt(`随机选择几只宝可梦？（1 - ${max}）`, {
    title,
    okText,
    icon: '🎲',
    inputType: 'number',
    inputValue: String(def),
    inputRequired: true
  })
  if (val === null) return null
  const n = Math.floor(Number(val))
  if (!Number.isFinite(n) || n < 1 || n > max) {
    $confirm.error(`数量需要在 1 到 ${max} 之间`)
    return null
  }
  return n
}

/** 只做随机选择：替换当前名单，等待教师再点「保存设置」 */
async function randomSelect() {
  const n = await askRandomCount('一键随机选择', '开始随机')
  if (!n) return
  if (selectedEntries.value.length > 0) {
    const ok = await $confirm.confirm(
      `当前已选 ${selectedEntries.value.length} 只，随机选择会替换这份名单（保存后才写入池子）。继续吗？`,
      { title: '替换当前选择', okText: '替换', icon: '🎲' }
    )
    if (!ok) return
  }
  randomBusy.value = true
  try {
    selectedEntries.value = pickRandomEntries(n)
    toast(`已随机选中 ${n} 只，记得点「保存设置」生效`, { icon: '🎲' })
  } finally {
    randomBusy.value = false
  }
}

/** 随机选择 + 保存池子 + 发放精灵球 */
async function randomSelectAndDistribute() {
  const n = await askRandomCount('一键随机发放精灵球', '下一步')
  if (!n) return

  if (!appStore.students?.length) {
    try { await appStore.fetchStudents() } catch (e) { /* 数量仅用于提示 */ }
  }
  const total = appStore.students?.length || 0

  const ok = await $confirm.confirm(
    `将把宝可梦池替换为随机选出的 ${n} 只，并给全班 ${total} 位学生各发放 1 个精灵球。确定吗？`,
    { title: '随机发放精灵球', okText: '开始发放', icon: '🔮' }
  )
  if (!ok) return

  randomBusy.value = true
  distributing.value = true
  try {
    selectedEntries.value = pickRandomEntries(n)
    const saved = await savePool({ quiet: true })
    if (!saved) {
      $confirm.error('宝可梦池保存失败，已中止发放精灵球')
      return
    }
    const { data } = await pokemonApi.distributeBalls(appStore.currentClassId)
    const affected = data?.studentsAffected ?? total
    toast(`池子已换成 ${n} 只，精灵球已发放给 ${affected} 位学生`, { icon: '🔮' })
  } catch (e) {
    $confirm.error(e.response?.data?.error || e.message || '发放失败')
  } finally {
    randomBusy.value = false
    distributing.value = false
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
    await pokemonApi.distributeBalls(appStore.currentClassId)
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
  if (e.target.dataset.fallback) return
  e.target.dataset.fallback = '1'
  e.target.src = 'data:image/svg+xml;utf8,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><rect width="80" height="80" fill="#e2e8f0"/><text x="50%" y="50%" font-size="14" fill="#94a3b8" text-anchor="middle" dominant-baseline="middle">无图</text></svg>'
  )
}
</script>

<style scoped>
.page-wrap { max-width: 1200px; margin: 0 auto; }

.container {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  padding: 16px 18px;
  box-shadow: var(--shadow-s);
}

.pool-count {
  font-size: 12px;
  color: var(--ink-3);
  white-space: nowrap;
}
.pool-btn { padding: 8px 10px; }

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
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 13.5px;
  color: var(--ink-2);
  line-height: 1.7;
}
.banner-sub { font-size: 12.5px; color: var(--ink-3); }

/* 一键随机 */
.quick-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 12px;
  margin-bottom: 16px;
  border: 1px dashed var(--line);
  border-radius: 10px;
  background: var(--surface-2, #FAF7F6);
}
.quick-actions .btn-ghost { padding: 8px 12px; font-size: 12.5px; }
.qa-accent { border-color: var(--brand-soft-2); color: var(--brand-deep); }
.qa-hint { font-size: 11.5px; line-height: 1.6; color: var(--ink-3); flex: 1 1 240px; }

@media (max-width: 768px) {
  .quick-actions .btn-txt { display: none; }
  .quick-actions { gap: 6px; }
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
  transition: opacity 0.2s ease;
}

.pokemon-sprite {
  opacity: 0;
}

.pokemon-sprite--loaded {
  opacity: 1;
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
