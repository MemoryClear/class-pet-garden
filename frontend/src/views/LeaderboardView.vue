<template>
  <div class="leaderboard-page">
    <div class="container">
      <!-- 顶部导航 -->
      <div class="navbar">
        <button class="nav-btn back" @click="router.push('/home')">← 返回</button>
        <h2 class="navbar-title">🏆 光荣榜</h2>
        <button class="nav-btn refresh" @click="loadData" :disabled="loading">🔄</button>
      </div>

      <!-- Tab 切换 -->
      <div class="tab-bar">
        <button
          class="tab-btn"
          :class="{ active: mode === 'current' }"
          @click="mode = 'current'; loadData()"
        >当前积分</button>
        <button
          class="tab-btn"
          :class="{ active: mode === 'total' }"
          @click="mode = 'total'; loadData()"
        >总积分</button>
      </div>

      <!-- 说明 -->
      <div class="mode-hint">
        <template v-if="mode === 'current'">
          💡 当前积分：学生现有的粮食数量
        </template>
        <template v-else>
          💡 总积分：历史所有加分 - 扣分（不含道具消耗）
        </template>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <span class="spinner">⏳</span> 加载中...
      </div>

      <!-- 空状态 -->
      <div v-else-if="displayList.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <div class="empty-text">还没有学生上榜</div>
        <div class="empty-hint">给学生加分后即可查看排名</div>
      </div>

      <!-- 有数据 -->
      <div v-else>
        <!-- 🥇🥈🥉 前三名 领奖台 -->
        <div class="podium" v-if="displayList.length >= 2">
          <!-- 第二名 -->
          <div class="podium-item silver" :class="{ 'no-pet': !displayList[1].petIcon }">
            <div class="podium-medal">🥈</div>
            <div class="podium-avatar">
              <span class="pet-emoji">{{ displayList[1].petIcon || '❓' }}</span>
            </div>
            <div class="podium-name">{{ displayList[1].name }}</div>
            <div class="podium-score">{{ getScore(displayList[1]) }} 分</div>
            <div class="podium-level" :class="`lv-${getLevel(displayList[1])}`">{{ getLevelTitle(displayList[1]) }}</div>
            <div class="podium-rank">2</div>
          </div>
          <!-- 第一名 -->
          <div class="podium-item gold" :class="{ 'no-pet': !displayList[0].petIcon }">
            <div class="podium-medal">🥇</div>
            <div class="crown-float">👑</div>
            <div class="podium-avatar champion">
              <span class="pet-emoji">{{ displayList[0].petIcon || '❓' }}</span>
            </div>
            <div class="podium-name">{{ displayList[0].name }}</div>
            <div class="podium-score">{{ getScore(displayList[0]) }} 分</div>
            <div class="podium-level" :class="`lv-${getLevel(displayList[0])}`">{{ getLevelTitle(displayList[0]) }}</div>
            <div class="podium-rank">1</div>
          </div>
          <!-- 第三名 -->
          <div class="podium-item bronze" v-if="displayList.length >= 3" :class="{ 'no-pet': !displayList[2].petIcon }">
            <div class="podium-medal">🥉</div>
            <div class="podium-avatar">
              <span class="pet-emoji">{{ displayList[2].petIcon || '❓' }}</span>
            </div>
            <div class="podium-name">{{ displayList[2].name }}</div>
            <div class="podium-score">{{ getScore(displayList[2]) }} 分</div>
            <div class="podium-level" :class="`lv-${getLevel(displayList[2])}`">{{ getLevelTitle(displayList[2]) }}</div>
            <div class="podium-rank">3</div>
          </div>
        </div>

        <!-- 只有一人时显示单人冠军 -->
        <div class="solo-champion" v-if="displayList.length === 1">
          <div class="crown-float">👑</div>
          <div class="solo-avatar">
            <span class="pet-emoji">{{ displayList[0].petIcon || '❓' }}</span>
          </div>
          <div class="solo-name">{{ displayList[0].name }}</div>
          <div class="solo-score">{{ getScore(displayList[0]) }} 分</div>
          <div class="podium-level" :class="`lv-${getLevel(displayList[0])}`">{{ getLevelTitle(displayList[0]) }}</div>
        </div>

        <!-- 第4名以后列表 -->
        <div class="rank-list" v-if="displayList.length > 3">
          <div
            class="rank-item"
            v-for="(stu, idx) in displayList.slice(3)"
            :key="stu.id"
            :class="{ 'no-pet': !stu.petIcon }"
            :style="{ animationDelay: idx * 0.05 + 's' }"
          >
            <div class="rank-number">{{ idx + 4 }}</div>
            <div class="rank-avatar">
              <span>{{ stu.petIcon || '❓' }}</span>
            </div>
            <div class="rank-info">
              <div class="rank-name">{{ stu.name }}</div>
              <div class="rank-level" :class="`lv-${getLevel(stu)}`">{{ getLevelTitle(stu) }}</div>
            </div>
            <div class="rank-score">
              <span class="score-val">{{ getScore(stu) }}</span>
              <span class="score-unit">分</span>
            </div>
            <!-- 分数条 -->
            <div class="rank-bar">
              <div class="rank-bar-fill" :style="{ width: getBarWidth(stu) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAppStore } from '../stores/app.js'
import { useRouter } from 'vue-router'

const appStore = useAppStore()
const router = useRouter()
const loading = ref(false)
const mode = ref('current') // 'current' | 'total'
const totalList = ref([])

const displayList = computed(() => {
  return mode.value === 'current' ? appStore.leaderboard : totalList.value
})

function getScore(stu) {
  return mode.value === 'current' ? (stu.food ?? 0) : (stu.totalScore ?? 0)
}

function getLevel(stu) {
  const food = stu.food ?? 0
  if (food >= 50) return 4
  if (food >= 25) return 3
  if (food >= 10) return 2
  return 1
}

function getLevelTitle(stu) {
  const lv = getLevel(stu)
  const titles = { 1: '幼崽期', 2: '少年期', 3: '成年期', 4: '传说期' }
  return titles[lv]
}

function getBarWidth(stu) {
  if (displayList.value.length === 0) return 0
  const maxScore = getScore(displayList.value[0])
  if (maxScore === 0) return 0
  return Math.max(8, (getScore(stu) / maxScore) * 100)
}

async function loadData() {
  loading.value = true
  try {
    if (mode.value === 'current') {
      await appStore.fetchLeaderboard()
    } else {
      totalList.value = await appStore.fetchTotalScoreLeaderboard()
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.leaderboard-page { min-height: 100vh; background: linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%); color: #fff; }
.container { max-width: 800px; margin: 0 auto; padding: 20px; }

/* 导航栏 */
.navbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; padding: 12px 20px; background: rgba(255,255,255,0.08); border-radius: 20px; backdrop-filter: blur(10px); }
.navbar-title { font-size: 20px; font-weight: 700; background: linear-gradient(135deg, #ffd700, #ff8c00); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.nav-btn { background: rgba(255,255,255,0.1); border: none; color: #fff; padding: 8px 16px; border-radius: 12px; cursor: pointer; font-size: 14px; transition: background 0.2s; }
.nav-btn:hover { background: rgba(255,255,255,0.2); }
.nav-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* Tab 栏 */
.tab-bar { display: flex; gap: 8px; margin-bottom: 12px; }
.tab-btn { flex: 1; padding: 10px 16px; background: rgba(255,255,255,0.06); border: 2px solid transparent; border-radius: 12px; color: #aaa; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.tab-btn:hover { background: rgba(255,255,255,0.1); }
.tab-btn.active { background: rgba(255,215,0,0.15); border-color: rgba(255,215,0,0.4); color: #ffd700; }

/* 模式说明 */
.mode-hint { font-size: 12px; color: #888; margin-bottom: 20px; padding: 8px 12px; background: rgba(255,255,255,0.05); border-radius: 8px; }

/* 加载 & 空状态 */
.loading-state, .empty-state { text-align: center; padding: 60px 20px; }
.spinner { font-size: 24px; animation: spin 1s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-text { font-size: 18px; color: #ccc; margin-bottom: 8px; }
.empty-hint { font-size: 14px; color: #888; }

/* ========== 领奖台 ========== */
.podium { display: flex; align-items: flex-end; justify-content: center; gap: 12px; margin-bottom: 40px; padding-top: 20px; }
.podium-item { text-align: center; border-radius: 20px; padding: 20px 16px 16px; position: relative; transition: transform 0.3s; animation: podium-in 0.6s ease-out both; }
.podium-item:hover { transform: translateY(-4px); }
.podium-item.gold { background: linear-gradient(180deg, rgba(255,215,0,0.2) 0%, rgba(255,140,0,0.1) 100%); border: 2px solid rgba(255,215,0,0.4); min-width: 140px; animation-delay: 0.1s; }
.podium-item.silver { background: linear-gradient(180deg, rgba(192,192,192,0.15) 0%, rgba(169,169,169,0.08) 100%); border: 2px solid rgba(192,192,192,0.3); min-width: 120px; animation-delay: 0s; }
.podium-item.bronze { background: linear-gradient(180deg, rgba(205,127,50,0.15) 0%, rgba(184,115,51,0.08) 100%); border: 2px solid rgba(205,127,50,0.3); min-width: 120px; animation-delay: 0.2s; }

@keyframes podium-in {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.podium-medal { font-size: 32px; margin-bottom: 8px; }
.crown-float { font-size: 28px; text-align: center; animation: crown-float 2s ease-in-out infinite; margin-bottom: -4px; }
@keyframes crown-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.podium-avatar { margin: 8px auto; }
.podium-avatar .pet-emoji { font-size: 52px; }
.podium-avatar.champion .pet-emoji { font-size: 64px; filter: drop-shadow(0 4px 12px rgba(255,215,0,0.5)); }
.podium-name { font-size: 15px; font-weight: 600; margin: 8px 0 4px; }
.podium-score { font-size: 22px; font-weight: 800; background: linear-gradient(135deg, #ffd700, #ff8c00); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.podium-level { font-size: 11px; padding: 2px 8px; border-radius: 8px; display: inline-block; margin-top: 6px; }
.podium-level.lv-1 { background: rgba(255,255,255,0.1); color: #aaa; }
.podium-level.lv-2 { background: rgba(96,165,250,0.2); color: #60a5fa; }
.podium-level.lv-3 { background: rgba(168,85,247,0.2); color: #a855f7; }
.podium-level.lv-4 { background: rgba(255,215,0,0.2); color: #ffd700; }
.podium-rank { position: absolute; bottom: -10px; left: 50%; transform: translateX(-50%); background: rgba(255,255,255,0.1); font-size: 11px; padding: 2px 10px; border-radius: 10px; font-weight: 600; }

/* 单人冠军 */
.solo-champion { text-align: center; padding: 40px 20px; background: linear-gradient(180deg, rgba(255,215,0,0.15) 0%, rgba(255,140,0,0.05) 100%); border-radius: 24px; border: 2px solid rgba(255,215,0,0.3); margin-bottom: 30px; animation: podium-in 0.6s ease-out both; }
.solo-avatar .pet-emoji { font-size: 64px; }
.solo-name { font-size: 18px; font-weight: 600; margin-top: 10px; }
.solo-score { font-size: 28px; font-weight: 800; background: linear-gradient(135deg, #ffd700, #ff8c00); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin: 4px 0; }

/* ========== 排名列表 ========== */
.rank-list { display: flex; flex-direction: column; gap: 8px; }
.rank-item { display: flex; align-items: center; gap: 12px; background: rgba(255,255,255,0.06); border-radius: 16px; padding: 12px 16px; transition: all 0.2s; animation: slide-in 0.4s ease-out both; position: relative; overflow: hidden; }
.rank-item:hover { background: rgba(255,255,255,0.1); transform: translateX(4px); }

@keyframes slide-in {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

.rank-number { font-size: 16px; font-weight: 700; color: #888; min-width: 28px; text-align: center; }
.rank-avatar { font-size: 28px; min-width: 36px; text-align: center; }
.rank-info { flex: 1; }
.rank-name { font-size: 14px; font-weight: 600; }
.rank-level { font-size: 11px; padding: 1px 6px; border-radius: 6px; display: inline-block; margin-top: 2px; }
.rank-level.lv-1 { background: rgba(255,255,255,0.08); color: #888; }
.rank-level.lv-2 { background: rgba(96,165,250,0.15); color: #60a5fa; }
.rank-level.lv-3 { background: rgba(168,85,247,0.15); color: #a855f7; }
.rank-level.lv-4 { background: rgba(255,215,0,0.15); color: #ffd700; }
.rank-score { text-align: right; min-width: 60px; }
.score-val { font-size: 18px; font-weight: 700; color: #ffd700; }
.score-unit { font-size: 12px; color: #999; margin-left: 2px; }
.rank-bar { position: absolute; bottom: 0; left: 0; right: 0; height: 3px; background: rgba(255,255,255,0.05); }
.rank-bar-fill { height: 100%; background: linear-gradient(90deg, #ffd700, #ff8c00); border-radius: 0 3px 0 0; transition: width 0.6s ease-out; }

/* 无宠物标记淡化 */
.no-pet { opacity: 0.7; }
</style>
