<template>
  <div class="student-home" :style="bgStyle">
    <div class="container">
      <!-- 顶部导航 -->
      <div class="navbar">
        <div class="navbar-left">
          <span class="greeting">👋 {{ myInfo.name || '同学' }}</span>
          <span class="student-no">学号: {{ myInfo.studentNo || '无' }}</span>
          <span class="available-food">🍖 可用: {{ myInfo.food || 0 }}</span>
          <span class="pokemon-balls">🎒 精灵球: {{ myInfo.pokemonBalls || 0 }}</span>
          <span class="evo-items-nav">💎 道具: {{ totalEvoItems }}</span>
        </div>
        <div class="navbar-right">
          <button class="nav-btn" :class="{ active: activeSection === 'pet' }" @click="activeSection='pet'">🐾 宠物</button>
          <button class="nav-btn" :class="{ active: activeSection === 'pokemon' }" @click="activeSection='pokemon'">🎮 宝可梦</button>
          <button class="nav-btn" :class="{ active: activeSection === 'leaderboard' }" @click="activeSection='leaderboard'">🏆 光荣榜</button>
          <button class="nav-btn" :class="{ active: activeSection === 'shop' }" @click="activeSection='shop'">🏪 小卖部</button>
          <button class="nav-btn" :class="{ active: activeSection === 'classroom' }" @click="activeSection='classroom'">📚 课堂</button>
          <button class="nav-btn" :class="{ active: activeSection === 'records' }" @click="activeSection='records'">📝 记录</button>
          <button class="nav-btn logout-btn" @click="handleLogout">退出</button>
        </div>
      </div>

      <!-- 普通宠物区（最多1个） -->
      <div v-if="activeSection === 'pet'" class="section pet-section">
        <div class="pet-card-large">
          <div class="pet-avatar-wrap">
            <PetIcon :icon="myInfo.petIcon" size="64" fallback="🥚" />
          </div>
          <div class="pet-info">
            <h2>{{ myInfo.petName || '还没有宠物' }}</h2>
            <div class="pet-level">{{ myInfo.level }}</div>
            <div class="pet-score">
              <span class="score-label">积分</span>
              <span class="score-value">{{ myInfo.food }}</span>
            </div>
            <div class="pet-cards-row">
              <span v-if="myInfo.petChangeCards > 0" class="pet-cards">
                🎫 宠物更换卡 x{{ myInfo.petChangeCards }}
              </span>
            </div>
          </div>
        </div>
        <!-- 普通宠物图鉴 -->
        <div class="pet-library">
          <h4>🐾 宠物图鉴</h4>
          <div class="pet-library-tabs">
            <button :class="{ active: petLibraryTab === 'pet' }" @click="petLibraryTab = 'pet'">普通宠物</button>
            <button :class="{ active: petLibraryTab === 'pokemon' }" @click="petLibraryTab = 'pokemon'">我的宝可梦</button>
          </div>
          <div class="pet-grid" v-if="petLibraryTab === 'pet'">
            <div v-for="p in petLibrary" :key="p.id" class="pet-thumb" :class="{ owned: myInfo.petId === p.id }">
              <span class="thumb-icon">{{ p.icon }}</span>
              <span class="thumb-name">{{ p.name }}</span>
              <button v-if="myInfo.petId !== p.id" class="adopt-btn" @click="adoptNormalPet(p)">
                {{ myInfo.petId ? '更换' : '领养' }}
              </button>
              <span v-else class="owned-tag">已领养</span>
            </div>
          </div>

          <!-- 我的宝可梦 Tab -->
          <div v-if="petLibraryTab === 'pokemon'" class="pokemon-mylist">
            <div v-if="pokemonList.length === 0" class="empty-pokemon">
              <p>📭 你还没有宝可梦</p>
              <p class="hint">使用精灵球可以捕捉宝可梦哦</p>
            </div>
            <div v-else class="pokemon-mylist-grid">
              <div
                v-for="poke in pokemonList"
                :key="poke.id"
                class="pokemon-mylist-item"
                :class="{ 'is-represent': myInfo.representPokemonId === poke.id }"
                @click="setMyRepresent(poke)"
              >
                <div class="mylist-img-wrap">
                  <img :src="poke.image" :alt="poke.name" class="mylist-img" />
                  <div v-if="myInfo.representPokemonId === poke.id" class="mylist-crown">⭐</div>
                </div>
                <div class="mylist-name">{{ poke.name }}</div>
                <div class="mylist-level">Lv.{{ poke.level }}</div>
                <div class="mylist-food">🍖 {{ poke.food }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>



      <!-- 宝可梦团队区（无上限） -->
      <div v-if="activeSection === 'pokemon'" class="section pokemon-section">
        <div class="pokemon-header">
          <h3>🎮 我的宝可梦团队</h3>
          <div class="pokemon-actions">
            <span class="pokemon-count">共 {{ pokemonList.length }} 只</span>
            <button class="use-ball-btn" @click="showBallModal = true" :disabled="myInfo.pokemonBalls <= 0">
              🎒 使用精灵球
            </button>
            <button class="feed-all-btn" @click="showFeedModal = true" :disabled="myInfo.food <= 0">
              🍖 分配食物
            </button>
            
          </div>
        </div>
        
        <div class="pokemon-team">
          <div v-for="poke in pokemonList" :key="poke.id" class="pokemon-card" :class="{ 'can-evolve': poke.canEvolve }">
            <div class="pokemon-avatar">
              <img v-if="poke.image" :src="poke.image" :alt="poke.name" class="pokemon-img" />
              <span v-else class="pokemon-icon-fallback">❓</span>
              <div v-if="poke.canEvolve" class="evolve-badge">✨可进化</div>
            </div>
            <div class="pokemon-info">
              <div class="pokemon-name">{{ poke.name }}</div>
              <div class="pokemon-types">
                <span v-for="t in parseTypes(poke.types)" :key="t" :class="'type-tag type-' + getTypeClass(t)">{{ t }}</span>
              </div>
              <div class="pokemon-stats">
                <span class="level-badge">Lv.{{ poke.level }}</span>
                <span class="food-count">🍖 {{ poke.food }}{{ poke.maxFood ? ' / ' + poke.maxFood : '' }}</span>
              </div>
              <!-- 进化进度条 -->
              <div v-if="poke.nextEvolution" class="evolution-progress">
                <div class="progress-label">
                  进化: {{ poke.nextEvolution.name }}（还需 {{ Math.max(0, poke.nextEvolution.foodRequired - poke.food) }} 食物）
                </div>
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: poke.nextEvolution.progress + '%' }"></div>
                </div>
                <div class="progress-text">{{ poke.food }} / {{ poke.nextEvolution.foodRequired }}</div>
              </div>
              <!-- 道具进化提示 -->
              <div v-if="poke.evolutionHint" class="evolution-item-hint">
                <span class="hint-icon">💎</span> {{ poke.evolutionHint }}
              </div>
              <!-- 道具进化选项 -->
              <div v-if="poke.itemEvolutions && poke.itemEvolutions.filter(x => x.implemented !== false).length > 0" class="item-evolutions">
                <div v-for="ie in poke.itemEvolutions" :key="ie.toPokedexId + '_' + ie.toFormName" class="item-evo-container" style="display:flex;flex-direction:column;gap:4px;font-size:0.78rem;padding:5px 8px;background:rgba(139,92,246,0.06);border-radius:6px">
                  <div class="item-evo-top-row" style="display:flex;align-items:center;justify-content:center;gap:6px;flex-wrap:wrap">
                    <span class="item-evo-arrow">→ {{ ie.toName }}</span>
                    <span class="item-evo-req">
                      <span v-for="ri in ie.requiredItems" :key="ri" class="required-item" :class="{ owned: hasEvolutionItem(ri) }">
                        {{ ri }}{{ hasEvolutionItem(ri) ? ' ✅' : '' }}
                      </span>
                    </span>
                  </div>
                  <div class="item-evo-bottom-row" style="display:flex;justify-content:center">
                    <template v-if="ie.implemented === false">
                      <div style="color:#9ca3af;font-size:0.75rem;font-style:italic;text-align:center">🌟 已达最终形态</div>
                    </template>
                    <template v-else>
                      <button v-if="ie.requiredItems && ie.requiredItems.some(ri => hasEvolutionItem(ri))" class="use-item-btn" @click="useEvolutionItem(poke, ie.requiredItems.find(ri => hasEvolutionItem(ri)))">
                        💎 使用进化
                      </button>
                      <span v-else-if="ie.requiredItems && ie.requiredItems.length > 0" style="color:#9ca3af;font-size:0.72rem">🪨 缺少道具</span>
                    </template>
                  </div>
                
                </div>
              </div>
                <div v-else-if="poke.isFinalForm" class="max-evolution">🌟 已达最终形态</div>
              <div v-else-if="poke.evolutionType === 'item'" class="max-evolution">💎 需要道具进化</div>
              <div v-else-if="poke.evolutionType === 'none'" class="max-evolution">无进化路线</div>
            </div>
            <div class="pokemon-actions">
              <button v-if="poke.canEvolve" class="evolve-btn" @click="evolvePokemon(poke)">
                🌟 进化
              </button>
              <button class="feed-btn" @click="feedSinglePokemon(poke)" :disabled="myInfo.food <= 0 || (poke.maxFood > 0 && poke.food >= poke.maxFood)">
                🍖+1
              </button>
            </div>
          </div>
          <!-- 空状态 -->
          <div v-if="pokemonList.length === 0" class="empty-pokemon">
            <div class="empty-icon">🎮</div>
            <p>还没有宝可梦，快去领养一只吧！</p>
            <button class="adopt-btn" @click="showPokemonModal = true">领养宝可梦</button>
          </div>
        </div>
      </div>

      <!-- 光荣榜 -->
      <div v-if="activeSection === 'leaderboard'" class="section">
        <div class="section-tabs">
          <button :class="{ active: lbTab === 'food' }" @click="lbTab='food'">零食排行</button>
          <button :class="{ active: lbTab === 'total' }" @click="lbTab='total'">总积分排行</button>
        </div>
        <div v-if="lbTab === 'food'" class="leaderboard-list">
          <div v-for="(s, i) in leaderboard" :key="s.id" class="lb-item" :class="{ me: s.id === myInfo.id }">
            <span class="lb-rank">{{ i < 3 ? ['🥇','🥈','🥉'][i] : (i+1) }}</span>
            <span class="lb-icon"><PetIcon :icon="s.petIcon" size="20" /></span>
            <span class="lb-name">{{ s.name }}</span>
            <span class="lb-score">{{ s.food }}分</span>
          </div>
        </div>
        <div v-if="lbTab === 'total'" class="leaderboard-list">
          <div v-for="(s, i) in totalLeaderboard" :key="s.id" class="lb-item" :class="{ me: s.id === myInfo.id }">
            <span class="lb-rank">{{ i < 3 ? ['🥇','🥈','🥉'][i] : (i+1) }}</span>
            <span class="lb-icon"><PetIcon :icon="s.petIcon" size="20" /></span>
            <span class="lb-name">{{ s.name }}</span>
            <span class="lb-score">{{ s.totalScore }}分</span>
          </div>
        </div>
      </div>

      <!-- 小卖部 -->
      <div v-if="activeSection === 'shop'" class="section">
        <div class="shop-header">
          <span>💰 我的积分: {{ myInfo.food }}</span>
          <span class="shop-bal" style="margin-left:12px">⚪ 精灵球: {{ myInfo.pokemonBalls || 0 }}</span>
        </div>
        <div class="shop-grid">
          <div v-for="item in shopItems" :key="item.id" class="shop-item">
            <span class="item-icon">{{ item.icon }}</span>
            <span class="item-name">{{ item.name }}</span>
            <span class="item-price">{{ item.price }}分</span>
            <button class="exchange-btn" :disabled="myInfo.food < item.price" @click="exchangeItem(item)">
              兑换
            </button>
          </div>
        </div>
      </div>

      <!-- 课堂 -->
      <div v-if="activeSection === 'classroom'" class="section">
        <ClassroomView />
      </div>

      <!-- 记录 -->
      <div v-if="activeSection === 'records'" class="section">
        <div class="section-tabs">
          <button :class="{ active: recordTab === 'score' }" @click="recordTab='score'">💰 积分明细</button>
          <button :class="{ active: recordTab === 'exchange' }" @click="recordTab='exchange'">🎁 道具明细</button>
        </div>
        <div v-if="recordTab === 'score'" class="records-list">
          <div v-for="r in scoreHistory" :key="r.id" class="record-item">
            <div class="record-info">
              <span class="record-icon">{{ r.scoreItemIcon || '📝' }}</span>
              <div class="record-detail">
                <span class="record-name">{{ r.scoreItemName }}</span>
                <span v-if="r.revoked" class="revoked-tag">已撤销</span>
              </div>
            </div>
            <div class="record-right">
              <span :class="['record-points', r.point >= 0 ? 'positive' : 'negative']">
                {{ r.point >= 0 ? '+' : '' }}{{ r.point }}
              </span>
              <span class="record-time">{{ formatTime(r.createdAt) }}</span>
            </div>
          </div>
          <div v-if="scoreHistory.length === 0" class="empty">暂无积分记录</div>
        </div>
        <div v-if="recordTab === 'exchange'" class="records-list">
          <div v-for="r in exchangeHistory" :key="r.id" class="record-item">
            <div class="record-info">
              <span class="record-icon">{{ r.itemIcon }}</span>
              <div class="record-detail">
                <span class="record-name">{{ r.itemName }}</span>
                <span v-if="r.giftFromName" class="gift-tag">🎁 来自 {{ r.giftFromName }}</span>
              </div>
            </div>
            <div class="record-right">
              <span class="record-points negative">-{{ r.foodSpent }}</span>
              <span class="record-time">{{ formatTime(r.createdAt) }}</span>
            </div>
          </div>
          <div v-if="exchangeHistory.length === 0" class="empty">暂无道具记录</div>
        </div>
      </div>
    </div>

    <!-- 宝可梦选择弹窗 -->
    <div v-if="showPokemonModal" class="modal-overlay" @click.self="showPokemonModal = false">
      <div class="modal-card pokemon-modal">
        <div class="modal-header">
          <h3>🎮 领养宝可梦</h3>
          <button class="modal-close" @click="showPokemonModal = false">×</button>
        </div>
        <div class="pokemon-grid-modal">
          <div v-for="poke in pokemonLibrary" :key="poke.id" 
               class="pokemon-option" 
               :class="{ selected: selectedPokemon && selectedPokemon.pokedexId === poke.pokedexId }"
               @click="selectedPokemon = poke">
            <img v-if="poke.image" :src="poke.image" :alt="poke.name" class="pokemon-img-modal" />
            <div class="pokemon-name-modal">{{ poke.name }}</div>
            <div class="pokemon-types-modal">
              <span v-for="t in parseTypes(poke.types)" :key="t" :class="'type-tag type-' + getTypeClass(t)">{{ t }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showPokemonModal = false">取消</button>
          <button class="btn-confirm" :disabled="!selectedPokemon || adopting" @click="adoptPokemon">
            {{ adopting ? '领养中...' : '确认领养' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 分配食物弹窗 -->
    <div v-if="showFeedModal" class="modal-overlay" @click.self="showFeedModal = false">
      <div class="modal-card feed-modal">
        <div class="modal-header">
          <h3>🍖 分配食物给宝可梦</h3>
          <button class="modal-close" @click="showFeedModal = false">×</button>
        </div>
        <div class="feed-content">
          <div class="available-food">可用食物: {{ myInfo.food }}</div>
          <div class="pokemon-feed-list">
            <div v-for="poke in feedablePokemon" :key="poke.id" class="pokemon-feed-item">
              <div class="poke-info">
                <img v-if="poke.image" :src="poke.image" :alt="poke.name" class="poke-thumb" />
                <span class="poke-name">{{ poke.name }}</span>
                <span class="poke-level">Lv.{{ poke.level }}</span>
              </div>
              <div class="feed-controls">
                <button class="feed-minus" @click="feedAmounts[poke.id] = Math.max(0, (feedAmounts[poke.id] || 0) - 1)" :disabled="(feedAmounts[poke.id] || 0) <= 0">-</button>
                <input type="number" v-model.number="feedAmounts[poke.id]" min="0" :max="Math.min(myInfo.food, (poke.maxFood || 0) - (poke.food || 0))" class="feed-input" />
                <button class="feed-plus" @click="feedAmounts[poke.id] = Math.min(Math.min(myInfo.food, (poke.maxFood || 0) - (poke.food || 0)), (feedAmounts[poke.id] || 0) + 1)" :disabled="totalFeedAmount >= myInfo.food">+</button>
              </div>
            </div>
          </div>
          <div class="feed-summary" :class="{ 'over-limit': totalFeedAmount > myInfo.food }">
            总计分配: {{ totalFeedAmount }} / {{ myInfo.food }}
            <span v-if="totalFeedAmount > myInfo.food" class="feed-warning">⚠️ 超出可用食物，已分配部分将自动调整</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showFeedModal = false">取消</button>
          <button class="btn-confirm" :disabled="totalFeedAmount === 0 || feeding" @click="feedSelectedPokemon">
            {{ feeding ? '分配中...' : '确认分配' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 精灵球弹窗 -->
    <div v-if="showBallModal" class="modal-overlay" @click.self="closeBallModalAndContinue">
      <div class="modal-card ball-modal">
        <!-- 抽取动画 -->
        <div v-if="!obtainedPokemon" class="ball-content">
          <div class="ball-animation">
            <div class="pokeball" :class="{ shaking: usingBall }" @click="usePokemonBall">
              <div class="pokeball-top"></div>
              <div class="pokeball-bottom"></div>
              <div class="pokeball-center">
                <div class="pokeball-button"></div>
              </div>
            </div>
          </div>
          <p class="ball-hint">{{ usingBall ? '🎲 抽取中...' : '点击精灵球开始抽取！' }}</p>
          <p class="ball-count">剩余 {{ myInfo.pokemonBalls }} 个精灵球</p>
        </div>
        <!-- 宝可梦展示 -->
        <div v-else class="ball-content pokemon-reveal">
          <p class="congrats">🎉 恭喜获得新宝可梦！</p>
          <div class="reveal-animation">
            <img v-if="obtainedPokemon.image" :src="obtainedPokemon.image" :alt="obtainedPokemon.name" class="revealed-pokemon-img" />
            <span v-else class="revealed-pokemon-icon">❓</span>
          </div>
          <h2>{{ obtainedPokemon.name }}</h2>
          <div class="pokemon-types">
            <span v-for="t in parseTypes(obtainedPokemon.types)" :key="t" :class="'type-tag type-' + getTypeClass(t)">{{ t }}</span>
          </div>
          <p class="ball-count">剩余 {{ myInfo.pokemonBalls }} 个精灵球</p>
          <div class="modal-footer">
            <button class="btn-confirm" @click="closeBallModalAndContinue">确定</button>
          </div>
        </div>
      </div>
    </div>
  </div>


</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useAuthStore } from '../stores/auth.js'
import { useRouter } from 'vue-router'
import api, { studentApi2, petApi } from '../api/index.js'
import ClassroomView from './ClassroomView.vue'
import $confirm from '../composables/useConfirmModal.js'
import PetIcon from '../components/PetIcon.vue'


const authStore = useAuthStore()
const router = useRouter()

const activeSection = ref('pokemon')
const lbTab = ref('food')
const myInfo = reactive({ food: 0, evolutionItems: {} })
const totalEvoItems = computed(() => { const items = myInfo.evolutionItems || {}; return Object.values(items).reduce((sum, v) => sum + (v || 0), 0) })
const leaderboard = ref([])
const totalLeaderboard = ref([])
const petLibrary = ref([])
const petLibraryTab = ref('pet') // 'pet' | 'pokemon'
const shopItems = ref([])
const recordTab = ref('score')
const scoreHistory = ref([])
const exchangeHistory = ref([])

// 宝可梦相关
const pokemonList = ref([])
const pokemonLibrary = ref([])
const showPokemonModal = ref(false)
const selectedPokemon = ref(null)
const adopting = ref(false)

// 喂食相关
const showFeedModal = ref(false)
const feedAmounts = ref({})
const feeding = ref(false)

// 精灵球相关
const showBallModal = ref(false)
const usingBall = ref(false)
const obtainedPokemon = ref(null)

const bgStyle = computed(() => ({
  backgroundColor: 'rgba(255,107,157,0.06)',
  minHeight: '100vh'
}))

const feedablePokemon = computed(() => pokemonList.value.filter(p => {
  if (p.isFinalForm) return false
  if (p.maxFood <= 0) return true
  return (p.food || 0) < p.maxFood
}))

const totalFeedAmount = computed(() => {
  return Object.values(feedAmounts.value).reduce((sum, v) => sum + (v || 0), 0)
})

async function fetchMyInfo() {
  try {
    const { data } = await api.get('/student/me')
    Object.assign(myInfo, data)
    // 解析 evolutionItems
    if (typeof myInfo.evolutionItems === 'string') {
      try { myInfo.evolutionItems = JSON.parse(myInfo.evolutionItems) } catch { myInfo.evolutionItems = {} }
    }
    // 获取精灵球数量
    try {
      const ballsRes = await studentApi2.getPokemonBalls()
      myInfo.pokemonBalls = ballsRes.data.balls || 0
    } catch (e) {
      console.error('获取精灵球数量失败', e)
    }
  } catch (e) {
    console.error('fetchMyInfo error', e)
  }
}

async function fetchLeaderboard() {
  try {
    const [r1, r2] = await Promise.all([
      api.get('/student/leaderboard'),
      api.get('/student/leaderboard/total')
    ])
    leaderboard.value = r1.data
    totalLeaderboard.value = r2.data
  } catch (e) {
    console.error('fetchLeaderboard error', e)
  }
}

async function fetchShop() {
  try {
    const { data } = await api.get('/student/shop')
    shopItems.value = data
  } catch (e) {
    console.error('fetchShop error', e)
  }
}

async function fetchPetLibrary() {
  try {
    const { data } = await api.get('/student/pets')
    petLibrary.value = data
  } catch (e) {
    console.error('fetchPetLibrary error', e)
  }
}

async function fetchMyPokemon() {
  try {
    const { data } = await studentApi2.getPokemon()
    pokemonList.value = data || []
  } catch (e) {
    console.error('fetchMyPokemon error', e)
  }
}

async function fetchPokemonLibrary() {
  try {
    const { data } = await petApi.getPokemon()
    pokemonLibrary.value = data || []
  } catch (e) {
    console.error('fetchPokemonLibrary error', e)
  }
}



async function setMyRepresent(poke) {
  if (myInfo.representPokemonId === poke.id) return
  if (!await $confirm.confirm('设置「' + poke.name + '」为代表宝可梦？')) return
  try {
    const res = await studentApi2.setRepresentPokemon(poke.id)
    Object.assign(myInfo, {
      representPokemonId: res.data.representPokemonId || poke.id,
      petIcon: res.data.petIcon || poke.image,
      petName: res.data.petName || poke.name,
      petChangeCards: res.data.petChangeCards ?? myInfo.petChangeCards
    })
    $confirm.success('设置成功！⭐')
  } catch (e) {
    $confirm.error(e.response?.data?.error || '设置失败')
  }
}

async function adoptNormalPet(pet) {
  if (myInfo.petId && myInfo.petChangeCards <= 0) {
    $confirm.alert('更换宠物需要先购买宠物更换卡！', {type:'warn'})
    return
  }
  if (myInfo.petId && !await $confirm.confirm('更换宠物需要消耗一张宠物更换卡，确定继续吗？')) return
  try {
    await studentApi2.adopt({ petId: pet.id, petName: pet.name, petIcon: pet.icon })
    $confirm.success('领养成功！🐾')
    await fetchMyInfo()
  } catch (e) {
    $confirm.error(e.response?.data?.error || '领养失败')
  }
}

async function adoptPokemon() {
  if (!selectedPokemon.value) return
  adopting.value = true
  try {
    await studentApi2.adoptPokemon({
      pokedexId: selectedPokemon.value.pokedexId,
      name: selectedPokemon.value.name,
      image: selectedPokemon.value.image,
      types: selectedPokemon.value.types
    })
    $confirm.success('领养宝可梦成功！🎮')
    showPokemonModal.value = false
    selectedPokemon.value = null
    await fetchMyPokemon()
  } catch (e) {
    $confirm.error(e.response?.data?.error || '领养失败')
  } finally {
    adopting.value = false
  }
}

async function feedSinglePokemon(poke) {
  if (myInfo.food <= 0) {
    $confirm.alert('没有可用食物', {type:'warn'})
    return
  }
  try {
    await studentApi2.feedPokemon(poke.id, 1)
    myInfo.food -= 1
    await fetchMyPokemon()
    $confirm.success('喂食成功！🍖')
  } catch (e) {
    $confirm.error(e.response?.data?.error || '喂食失败')
  }
}

async function feedSelectedPokemon() {
  feeding.value = true
  let remainingFood = myInfo.food
  try {
    const entries = Object.entries(feedAmounts.value).filter(([id, amount]) => amount > 0)
    for (const [id, amount] of entries) {
      if (remainingFood <= 0) break
      // 获取该宝可梦的食物上限，超出上限的自动截断
      const poke = pokemonList.value.find(p => p.id === id)
      const maxFood = poke?.maxFood || 0
      const currentFood = poke?.food || 0
      const allowed = maxFood > 0 ? Math.min(amount, remainingFood, Math.max(0, maxFood - currentFood)) : Math.min(amount, remainingFood)
      if (allowed <= 0) break
      await studentApi2.feedPokemon(id, allowed)
      remainingFood -= allowed
    }
    feedAmounts.value = {}
    showFeedModal.value = false
    await fetchMyInfo()
    await fetchMyPokemon()
    $confirm.success('食物分配完成！🍖')
  } catch (e) {
    $confirm.error(e.response?.data?.error || '分配失败')
  } finally {
    feeding.value = false
  }
}

async function evolvePokemon(poke) {
  if (!await $confirm.confirm(`确定让 ${poke.name} 进化为 ${poke.nextEvolution?.name || '???'} 吗？`)) return
  
  try {
    await studentApi2.evolvePokemon(poke.id, {
      toPokedexId: poke.nextEvolution?.toPokedexId,
      toFormName: poke.nextEvolution?.toFormName || null
    })
    $confirm.success('进化成功！🌟')
    await fetchMyPokemon()
  } catch (e) {
    $confirm.error(e.response?.data?.error || '进化失败')
  }
}

async function useEvolutionItem(poke, itemKey) {
  const itemName = itemKey
  if (!await $confirm.confirm(`确定对 ${poke.name} 使用「${itemName}」进行进化吗？`)) return
  
  try {
    const { data } = await studentApi2.useEvolutionItem(poke.id, itemKey)
    $confirm.success('进化成功！🌟')
    // 更新本地道具库存
    if (data.evolutionItems) {
      myInfo.evolutionItems = data.evolutionItems
    } else {
      await fetchMyInfo()
    }
    await fetchMyPokemon()
  } catch (e) {
    $confirm.error(e.response?.data?.error || '使用道具失败')
  }
}

// 检查学生是否拥有某个进化道具
function hasEvolutionItem(itemName) {
  const items = myInfo.evolutionItems || {}
  return (items[itemName] || 0) > 0
}

async function exchangeItem(item) {
  if (!await $confirm.confirm('确定花费 ' + item.price + ' 分兑换「' + item.name + '」吗？')) return
  try {
    await studentApi2.exchange(item.id)
    $confirm.success('兑换成功！')
    await fetchMyInfo()
    await fetchShop()
    await fetchPokemonBalls()
  } catch (e) {
    $confirm.error(e.response?.data?.error || '兑换失败')
  }
}

function formatTime(t) {
  if (!t) return ''
  const s = typeof t === 'string' ? t : t.toString()
  return s.replace('T', ' ').substring(0, 16)
}

async function fetchRecords() {
  try {
    const [r1, r2] = await Promise.all([
      api.get('/student/score-history'),
      api.get('/student/exchange-history')
    ])
    scoreHistory.value = r1.data || []
    exchangeHistory.value = r2.data || []
  } catch (e) {
    console.error('fetchRecords error', e)
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

function parseTypes(types) {
  if (!types) return []
  try {
    return JSON.parse(types)
  } catch {
    return []
  }
}

function getTypeClass(type) {
  const map = {
    '草': 'grass', '火': 'fire', '水': 'water', '电': 'electric',
    '一般': 'normal', '飞行': 'flying', '毒': 'poison', '虫': 'bug',
    '妖精': 'fairy', '超能力': 'psychic', '格斗': 'fighting',
    '岩石': 'rock', '地面': 'ground', '幽灵': 'ghost', '龙': 'dragon',
    '恶': 'dark', '钢': 'steel', '冰': 'ice'
  }
  return map[type] || 'normal'
}

async function usePokemonBall() {
  if (myInfo.pokemonBalls <= 0) {
    $confirm.alert('没有可用的精灵球！', {type:'warn'})
    return
  }
  
  usingBall.value = true
  try {
    const { data } = await studentApi2.usePokemonBall()
    obtainedPokemon.value = data
    myInfo.pokemonBalls -= 1
    $confirm.success('获得了一只新的宝可梦！🎮')
    await fetchMyPokemon()
  } catch (e) {
    $confirm.error(e.response?.data?.error || '使用精灵球失败')
  } finally {
    usingBall.value = false
  }
}

function closeBallModalAndContinue() {
  showBallModal.value = false
  obtainedPokemon.value = null
}

onMounted(async () => {
  await Promise.all([
    fetchMyInfo(), 
    fetchLeaderboard(), 
    fetchPetLibrary(), 
    fetchShop(), 
    fetchRecords(),
    fetchMyPokemon(),
    fetchPokemonLibrary()
  ])
})
</script>

<style scoped>
.student-home { min-height: 100vh; }
.container { max-width: 900px; margin: 0 auto; padding: 20px; }
.navbar { background: #fff; border-radius: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); padding: 12px 20px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-bottom: 24px; }
.navbar-left { display: flex; align-items: center; gap: 12px; }
.greeting { font-weight: 600; font-size: 1.1rem; }
.student-no { background: #f0f0f0; padding: 4px 12px; border-radius: 12px; font-size: 0.85rem; color: #666; }
.available-food { background: #fff0f3; padding: 4px 12px; border-radius: 12px; font-size: 0.85rem; color: #ff6b9d; font-weight: 600; }
.navbar-right { display: flex; gap: 6px; flex-wrap: wrap; }
.nav-btn { background: transparent; border: none; padding: 6px 12px; border-radius: 10px; cursor: pointer; font-size: 0.9rem; }
.nav-btn:hover { background: #f5f5f5; }
.nav-btn.active { background: #ff6b9d; color: #fff; }
.logout-btn { color: #f44; }
.section { background: #fff; border-radius: 20px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }

/* 宠物区 */
.pet-card-large { display: flex; align-items: center; gap: 24px; padding: 20px; background: linear-gradient(135deg, #fff5f7, #fff); border-radius: 16px; border: 2px solid #ffe0e8; margin-bottom: 20px; }
.pet-avatar-wrap { width: 80px; height: 80px; display: flex; align-items: center; justify-content: center; }
.pet-icon { font-size: 64px; }
.pet-icon-img { width: 64px; height: 64px; object-fit: contain; }
.pet-info h2 { margin: 0 0 8px; font-size: 1.4rem; }
.pet-level { display: inline-block; padding: 4px 14px; background: #fff0f3; color: #ff6b9d; border-radius: 12px; font-size: 0.9rem; margin-bottom: 10px; }
.pet-score { display: flex; align-items: baseline; gap: 6px; }
.score-label { color: #888; font-size: 0.9rem; }
.score-value { font-size: 2rem; font-weight: 700; color: #ff6b9d; }
.pet-cards { margin-top: 8px; color: #8b5cf6; font-size: 0.9rem; }
.pet-library h4 { margin: 0 0 12px; }
.pet-library-tabs { display: flex; gap: 8px; margin: 8px 0; }
.pet-library-tabs button { flex: 1; padding: 6px 12px; border: 1px solid #e2e8f0; border-radius: 20px; background: #f8fafc; font-size: 13px; cursor: pointer; transition: all 0.2s; }
.pet-library-tabs button.active { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border-color: transparent; font-weight: 600; }
.pokemon-mylist { padding: 4px 0; }
.empty-pokemon { text-align: center; padding: 32px; color: #94a3b8; }
.empty-pokemon .hint { font-size: 13px; margin-top: 8px; }
.pokemon-mylist-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 10px; }
.pokemon-mylist-item { background: #f8fafc; border: 2px solid #e2e8f0; border-radius: 12px; padding: 8px 6px; cursor: pointer; transition: all 0.2s; text-align: center; }
.pokemon-mylist-item:hover { border-color: #667eea; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.pokemon-mylist-item.is-represent { border-color: #f59e0b; background: #fffbeb; }
.mylist-img-wrap { position: relative; width: 70px; height: 70px; margin: 0 auto 4px; }
.mylist-img { width: 100%; height: 100%; object-fit: contain; }
.mylist-crown { position: absolute; top: -4px; right: -4px; font-size: 16px; background: #fbbf24; width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.mylist-name { font-size: 12px; font-weight: 600; color: #1e293b; }
.mylist-level { font-size: 11px; color: #667eea; }
.mylist-food { font-size: 10px; color: #94a3b8; }

.pet-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(80px, 1fr)); gap: 10px; }
.pet-thumb { text-align: center; padding: 10px; border-radius: 12px; border: 2px solid #f0f0f0; transition: all 0.2s; }
.pet-thumb.owned { border-color: #ff6b9d; background: #fff5f7; }
.thumb-icon { font-size: 28px; display: block; }
.thumb-name { font-size: 0.75rem; color: #666; }
.adopt-btn { display: block; width: 100%; margin-top: 4px; padding: 2px 6px; background: #ff6b9d; color: #fff; border: none; border-radius: 6px; font-size: 0.7rem; cursor: pointer; }
.owned-tag { display: block; margin-top: 4px; font-size: 0.7rem; color: #ff6b9d; }

/* 宝可梦团队区 */
.pokemon-section { }
.pokemon-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
.pokemon-header h3 { margin: 0; }
.pokemon-actions { display: flex; gap: 10px; align-items: center; }
.pokemon-count { color: #666; }
.feed-all-btn { background: #fff0f3; border: 1px solid #ff6b9d; color: #ff6b9d; padding: 6px 12px; border-radius: 8px; cursor: pointer; }
.feed-all-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.pokemon-team { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }
.pokemon-card { background: linear-gradient(135deg, #f0f9ff, #fff); border-radius: 16px; border: 2px solid #e0f2fe; padding: 16px; position: relative; }
.pokemon-card.can-evolve { border-color: #ffd700; background: linear-gradient(135deg, #fffbeb, #fff); animation: glow 1.5s ease-in-out infinite alternate; }
@keyframes glow { from { box-shadow: 0 0 5px rgba(255,215,0,0.3); } to { box-shadow: 0 0 15px rgba(255,215,0,0.6); } }

.pokemon-avatar { position: relative; text-align: center; margin-bottom: 12px; }
.pokemon-img { width: 80px; height: 80px; object-fit: contain; }
.pokemon-icon-fallback { font-size: 60px; }
.evolve-badge { position: absolute; top: -5px; right: calc(50% - 50px); background: #ffd700; color: #b8860b; padding: 2px 8px; border-radius: 10px; font-size: 0.7rem; font-weight: bold; animation: pulse 1s infinite; }
@keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.05); } }

.pokemon-info { text-align: center; }
.pokemon-name { font-weight: 700; font-size: 1.1rem; margin-bottom: 6px; }
.pokemon-types { display: flex; justify-content: center; gap: 4px; margin-bottom: 8px; flex-wrap: wrap; }
.type-tag { padding: 2px 8px; border-radius: 4px; font-size: 0.75rem; color: #fff; }
.type-grass { background: #22c55e; }
.type-fire { background: #f97316; }
.type-water { background: #3b82f6; }
.type-electric { background: #eab308; }
.type-normal { background: #a1a1aa; }
.type-flying { background: #8b5cf6; }
.type-poison { background: #a855f7; }
.type-bug { background: #84cc16; }
.type-fairy { background: #ec4899; }
.type-psychic { background: #f472b6; }
.type-fighting { background: #dc2626; }
.type-rock { background: #92400e; }
.type-ground { background: #d97706; }
.type-ghost { background: #7c3aed; }
.type-dragon { background: #6366f1; }
.type-dark { background: #374151; }
.type-steel { background: #6b7280; }
.type-ice { background: #06b6d4; }

.pokemon-stats { display: flex; justify-content: center; gap: 12px; margin-bottom: 10px; }
.level-badge { background: #dbeafe; color: #1e40af; padding: 2px 10px; border-radius: 10px; font-size: 0.85rem; font-weight: 600; }
.food-count { color: #888; font-size: 0.9rem; }

.evolution-progress { margin-top: 10px; }
.progress-label { font-size: 0.8rem; color: #666; margin-bottom: 4px; }
.progress-bar { height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #ffd700, #ff8c00); border-radius: 3px; transition: width 0.3s; }
.progress-text { font-size: 0.75rem; color: #888; text-align: right; }
.max-evolution { font-size: 0.85rem; color: #22c55e; text-align: center; }
  .evolution-item-hint { font-size: 0.8rem; color: #8b5cf6; text-align: center; margin: 4px 0; background: rgba(139,92,246,0.1); border-radius: 6px; padding: 5px 10px; word-break: break-word; }
  .evolution-item-hint .hint-icon { margin-right: 2px; }
  .item-evolutions { margin: 4px 0; display: flex; flex-direction: column; gap: 4px; }
  .item-evo-container { display: flex; flex-direction: column; gap: 4px; font-size: 0.78rem; padding: 5px 8px; background: rgba(139,92,246,0.06); border-radius: 6px; }
  .item-evo-top-row { display: flex; align-items: center; justify-content: center; gap: 6px; flex-wrap: wrap; }
  .item-evo-arrow { color: #fbbf24; font-weight: 600; white-space: nowrap; flex-shrink: 0; }
  .item-evo-arrow + .item-evo-req { margin-left: 2px; }
  .item-evo-req { display: flex; gap: 4px; flex-wrap: wrap; flex: 1; min-width: 0; }
  .item-evo-bottom-row { display: flex; justify-content: center; }
  .required-item { background: rgba(239,68,68,0.15); color: #f87171; padding: 2px 6px; border-radius: 3px; font-size: 0.72rem; white-space: nowrap; flex-shrink: 0; }
  .required-item.owned { background: rgba(34,197,94,0.15); color: #22c55e; }
  .use-item-btn { background: linear-gradient(135deg, #8b5cf6, #6366f1); color: white; border: none; padding: 4px 10px; border-radius: 6px; font-size: 0.72rem; cursor: pointer; flex-shrink: 0; white-space: nowrap; }
  .use-item-btn:hover { filter: brightness(1.15); }
  .evo-items-nav { font-size: 0.82rem; color: #a78bfa; }

.pokemon-card .pokemon-actions { display: flex; justify-content: center; gap: 8px; margin-top: 10px; }
.evolve-btn { background: linear-gradient(135deg, #ffd700, #ff8c00); color: #fff; border: none; padding: 6px 14px; border-radius: 12px; cursor: pointer; font-weight: 600; font-size: 0.85rem; }
.evolve-btn:hover { transform: scale(1.05); }
.feed-btn { background: #fff0f3; border: 1px solid #ff6b9d; color: #ff6b9d; padding: 6px 12px; border-radius: 12px; cursor: pointer; font-size: 0.85rem; }
.feed-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.empty-pokemon { grid-column: 1 / -1; text-align: center; padding: 40px; color: #888; }
.empty-icon { font-size: 64px; margin-bottom: 16px; }

/* 弹窗 */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal-card { background: #fff; border-radius: 16px; max-width: 90vw; max-height: 80vh; overflow: hidden; }
.pokemon-modal { width: 700px; }
.feed-modal { width: 500px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #eee; }
.modal-header h3 { margin: 0; }
.modal-close { background: none; border: none; font-size: 24px; cursor: pointer; color: #888; }

.pokemon-grid-modal { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; padding: 16px; max-height: 400px; overflow-y: auto; }
.pokemon-option { padding: 10px; border-radius: 10px; border: 2px solid #eee; text-align: center; cursor: pointer; transition: all 0.2s; }
.pokemon-option:hover { border-color: #3b82f6; background: #f0f9ff; }
.pokemon-option.selected { border-color: #3b82f6; background: #dbeafe; }
.pokemon-img-modal { width: 56px; height: 56px; object-fit: contain; margin-bottom: 4px; }
.pokemon-name-modal { font-size: 0.85rem; font-weight: 500; margin-bottom: 4px; }
.pokemon-types-modal { display: flex; justify-content: center; gap: 2px; flex-wrap: wrap; }

.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 20px; border-top: 1px solid #eee; }
.btn-cancel { padding: 8px 20px; background: #f5f5f5; border: none; border-radius: 8px; cursor: pointer; }
.btn-confirm { padding: 8px 20px; background: #3b82f6; color: #fff; border: none; border-radius: 8px; cursor: pointer; }
.btn-confirm:disabled { background: #ccc; cursor: not-allowed; }

/* 分配食物弹窗 */
.feed-content { padding: 16px 20px; }
.pokemon-feed-list { max-height: 300px; overflow-y: auto; }
.pokemon-feed-item { display: flex; justify-content: space-between; align-items: center; padding: 10px; border-radius: 8px; background: #f9fafb; margin-bottom: 8px; }
.poke-info { display: flex; align-items: center; gap: 10px; }
.poke-thumb { width: 40px; height: 40px; object-fit: contain; }
.poke-name { font-weight: 500; }
.poke-level { color: #888; font-size: 0.85rem; }
.feed-controls { display: flex; align-items: center; gap: 4px; }
.feed-input { width: 50px; text-align: center; border: 1px solid #ddd; border-radius: 4px; padding: 4px; }
.feed-minus, .feed-plus { width: 28px; height: 28px; border: 1px solid #ddd; background: #fff; border-radius: 4px; cursor: pointer; }
.feed-minus:disabled, .feed-plus:disabled { opacity: 0.5; cursor: not-allowed; }
.feed-summary { margin-top: 16px; padding: 10px; background: #fff0f3; border-radius: 8px; text-align: center; font-weight: 600; color: #ff6b9d; }
.feed-summary.over-limit { background: #fff3cd; color: #856404; border: 1px solid #ffc107; }
.feed-warning { display: block; font-size: 12px; font-weight: 400; margin-top: 4px; }

/* 其他样式保持不变 */
.section-tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.section-tabs button { padding: 8px 18px; border: 1px solid #ddd; border-radius: 16px; background: #fff; cursor: pointer; }
.section-tabs button.active { background: #ff6b9d; color: #fff; border-color: #ff6b9d; }
.leaderboard-list { }
.lb-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border-radius: 12px; margin-bottom: 6px; }
.lb-item.me { background: #fff0f3; border: 2px solid #ff6b9d; }
.lb-rank { width: 32px; text-align: center; font-weight: 600; }
.lb-icon { font-size: 24px; }
.lb-name { flex: 1; }
.lb-score { font-weight: 600; color: #ff6b9d; }

.shop-header { margin-bottom: 16px; font-size: 1.1rem; font-weight: 600; }
.shop-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 12px; }
.shop-item { background: #fafafa; border-radius: 12px; padding: 14px; text-align: center; }
.item-icon { font-size: 28px; display: block; margin-bottom: 6px; }
.item-name { display: block; font-weight: 500; margin-bottom: 4px; }
.item-price { display: block; color: #ff6b9d; font-size: 0.9rem; margin-bottom: 8px; }
.exchange-btn { padding: 6px 16px; background: #ff6b9d; color: #fff; border: none; border-radius: 12px; cursor: pointer; }
.exchange-btn:disabled { background: #ddd; cursor: not-allowed; }

.records-list { max-height: 500px; overflow-y: auto; }
.record-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; border-radius: 12px; margin-bottom: 6px; background: #fafafa; }
.record-info { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.record-icon { font-size: 1.2rem; }
.record-detail { display: flex; flex-direction: column; min-width: 0; }
.record-name { font-weight: 500; font-size: 0.95rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.record-right { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
.record-points { font-weight: 700; font-size: 1.1rem; }
.record-points.positive { color: #059669; }
.record-points.negative { color: #dc2626; }
.record-time { font-size: 0.75rem; color: #999; }
.revoked-tag { color: #999; font-size: 0.8rem; text-decoration: line-through; }
.gift-tag { color: #8b5cf6; font-size: 0.85rem; }
.empty { text-align: center; padding: 20px; color: #888; }

/* 精灵球样式 */
.pokemon-balls { background: #e0f2fe; padding: 4px 12px; border-radius: 12px; font-size: 0.85rem; color: #0369a1; font-weight: 600; }
.use-ball-btn { background: linear-gradient(135deg, #3b82f6, #1d4ed8); color: #fff; border: none; padding: 6px 12px; border-radius: 8px; cursor: pointer; font-weight: 600; }
.use-ball-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.ball-modal { width: 400px; }
.ball-content { padding: 30px; text-align: center; }
.ball-animation { }
.pokeball { width: 120px; height: 120px; margin: 0 auto 20px; position: relative; cursor: pointer; transition: transform 0.1s; }
.pokeball:hover { transform: scale(1.05); }
.pokeball.shaking { animation: shake 0.5s ease-in-out infinite; }
@keyframes shake { 0%, 100% { transform: rotate(0deg); } 25% { transform: rotate(-10deg); } 75% { transform: rotate(10deg); } }
.pokeball-top { position: absolute; top: 0; left: 0; right: 0; height: 50%; background: linear-gradient(180deg, #ef4444, #dc2626); border-radius: 60px 60px 0 0; border: 4px solid #1f2937; }
.pokeball-bottom { position: absolute; bottom: 0; left: 0; right: 0; height: 50%; background: linear-gradient(180deg, #f3f4f6, #e5e7eb); border-radius: 0 0 60px 60px; border: 4px solid #1f2937; }
.pokeball-center { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 40px; height: 40px; background: #1f2937; border-radius: 50%; display: flex; align-items: center; justify-content: center; z-index: 1; }
.pokeball-button { width: 24px; height: 24px; background: #fff; border-radius: 50%; border: 4px solid #1f2937; }
.ball-hint { font-size: 1.1rem; color: #666; margin-bottom: 10px; }
.ball-count { font-size: 0.9rem; color: #888; }

.pokemon-reveal { }
.reveal-animation { animation: revealBounce 0.6s ease-out; }
@keyframes revealBounce { 0% { transform: scale(0); opacity: 0; } 50% { transform: scale(1.1); } 100% { transform: scale(1); opacity: 1; } }
.revealed-pokemon-img { width: 150px; height: 150px; object-fit: contain; margin-bottom: 10px; animation: float 2s ease-in-out infinite; }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }
.pokemon-reveal h2 { margin: 10px 0; font-size: 1.8rem; color: #1f2937; }
.pokemon-reveal .pokemon-types { display: flex; justify-content: center; gap: 6px; margin-bottom: 16px; }
.congrats { font-size: 1.2rem; color: #22c55e; font-weight: 600; }


  .unimplemented-hint {
    font-size: 0.75rem;
    color: #999;
    text-align: center;
    padding: 4px 8px;
    background: rgba(0,0,0,0.05);
    border-radius: 4px;
    margin-top: 4px;
  }

    .pet-cards-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-top: 4px; }
    .pet-name { font-size: 0.75rem; color: #374151; text-align: center; }

    </style>
