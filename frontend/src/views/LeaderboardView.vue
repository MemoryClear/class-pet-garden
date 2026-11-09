<template>
  <AppShell title="光荣榜" :subtitle="subtitle">
    <template #actions>
      <button class="icon-btn" :disabled="loading" title="刷新" @click="loadData">
        {{ loading ? '⏳' : '🔄' }}
      </button>
    </template>

    <div class="page-wrap">
      <!-- 榜单口径 -->
      <div class="toolbar">
        <div class="seg">
          <button :class="{ on: mode === 'current' }" @click="switchMode('current')">当前积分</button>
          <button :class="{ on: mode === 'total' }" @click="switchMode('total')">总积分</button>
        </div>
        <span class="hint">
          {{ mode === 'current' ? '💡 当前积分 = 学生现有的口粮数量' : '💡 总积分 = 历史加分 − 扣分（不含道具消耗）' }}
        </span>
      </div>

      <!-- 加载 -->
      <div v-if="loading" class="state">
        <div class="state-icon spin">⏳</div>
        <div class="state-text">加载中…</div>
      </div>

      <!-- 空 -->
      <div v-else-if="displayList.length === 0" class="state">
        <div class="state-icon">📭</div>
        <div class="state-text">还没有学生上榜</div>
        <div class="state-hint">给学生加分后即可查看排名</div>
      </div>

      <template v-else>
        <!-- 前三名 -->
        <div class="podium" v-if="displayList.length >= 2">
          <div class="podium-item silver">
            <div class="podium-medal">🥈</div>
            <div class="podium-avatar">
              <PetIcon :icon="displayList[1].petIcon" size="40" />
            </div>
            <div class="podium-name">{{ displayList[1].name }}</div>
            <div class="podium-score">{{ getScore(displayList[1]) }}<i>分</i></div>
            <div class="podium-level">{{ getLevelName(displayList[1]) }}</div>
            <div class="podium-stand">2</div>
          </div>

          <div class="podium-item gold">
            <div class="crown">👑</div>
            <div class="podium-medal">🥇</div>
            <div class="podium-avatar champion">
              <PetIcon :icon="displayList[0].petIcon" size="56" />
            </div>
            <div class="podium-name">{{ displayList[0].name }}</div>
            <div class="podium-score">{{ getScore(displayList[0]) }}<i>分</i></div>
            <div class="podium-level">{{ getLevelName(displayList[0]) }}</div>
            <div class="podium-stand">1</div>
          </div>

          <div class="podium-item bronze" v-if="displayList.length >= 3">
            <div class="podium-medal">🥉</div>
            <div class="podium-avatar">
              <PetIcon :icon="displayList[2].petIcon" size="40" />
            </div>
            <div class="podium-name">{{ displayList[2].name }}</div>
            <div class="podium-score">{{ getScore(displayList[2]) }}<i>分</i></div>
            <div class="podium-level">{{ getLevelName(displayList[2]) }}</div>
            <div class="podium-stand">3</div>
          </div>
        </div>

        <!-- 单人冠军 -->
        <div class="solo" v-if="displayList.length === 1">
          <div class="crown">👑</div>
          <div class="podium-avatar champion">
            <PetIcon :icon="displayList[0].petIcon" size="56" />
          </div>
          <div class="solo-name">{{ displayList[0].name }}</div>
          <div class="podium-score">{{ getScore(displayList[0]) }}<i>分</i></div>
          <div class="podium-level">{{ getLevelName(displayList[0]) }}</div>
        </div>

        <!-- 第 4 名以后 -->
        <div class="rank-list" v-if="displayList.length > 3">
          <div class="rank-item" v-for="(stu, idx) in displayList.slice(3)" :key="stu.id">
            <div class="rank-number">{{ idx + 4 }}</div>
            <div class="rank-avatar">
              <PetIcon :icon="stu.petIcon" size="22" />
            </div>
            <div class="rank-info">
              <div class="rank-name">{{ stu.name }}</div>
              <div class="rank-level">{{ getLevelName(stu) }}</div>
            </div>
            <div class="rank-score">{{ getScore(stu) }}<i>分</i></div>
            <div class="rank-bar">
              <div class="rank-bar-fill" :style="{ width: getBarWidth(stu) + '%' }"></div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useAppStore } from '../stores/app.js'
import AppShell from '../components/layout/AppShell.vue'
import PetIcon from '../components/PetIcon.vue'
import { levelOf, levelInfo } from '../constants/petLevels.js'

const appStore = useAppStore()
const loading = ref(false)
const mode = ref('current') // 'current' | 'total'
const totalList = ref([])

const displayList = computed(() => (mode.value === 'current' ? appStore.leaderboard : totalList.value))

const subtitle = computed(() => {
  const n = displayList.value.length
  if (loading.value) return '正在统计…'
  if (!n) return '先给学生加分，榜单才会热闹起来'
  return `${n} 位同学上榜 · ${mode.value === 'current' ? '按当前口粮排名' : '按历史总分排名'}`
})

function getScore(stu) {
  return mode.value === 'current' ? (stu.food ?? 0) : (stu.totalScore ?? 0)
}

function getLevelName(stu) {
  return levelInfo(levelOf(stu.food)).name
}

function getBarWidth(stu) {
  if (displayList.value.length === 0) return 0
  const maxScore = getScore(displayList.value[0])
  if (maxScore === 0) return 0
  return Math.max(8, (getScore(stu) / maxScore) * 100)
}

function switchMode(m) {
  if (mode.value === m) return
  mode.value = m
  loadData()
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

// 切换班级后重新取榜
watch(() => appStore.currentClassId, () => { if (appStore.currentClassId) loadData() })
</script>

<style scoped>
.page-wrap { max-width: 860px; margin: 0 auto; }

.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; flex-wrap: wrap; }
.seg { display: flex; background: #F4EFEE; padding: 4px; border-radius: 11px; }
.seg button {
  border: 0;
  background: transparent;
  padding: 7px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  color: #767D8C;
  cursor: pointer;
  transition: .16s;
  touch-action: manipulation;
}
.seg button.on { background: var(--surface); color: var(--ink); font-weight: 600; box-shadow: 0 1px 3px rgba(0, 0, 0, .07); }
.hint { font-size: 12px; color: var(--ink-3); }

/* 状态 */
.state {
  text-align: center;
  padding: 56px 20px;
  background: var(--surface);
  border: 1px dashed var(--line);
  border-radius: var(--r-lg);
}
.state-icon {
  width: 72px; height: 72px;
  margin: 0 auto 14px;
  border-radius: 50%;
  background: var(--line-soft);
  display: flex; align-items: center; justify-content: center;
  font-size: 30px;
}
.state-icon.spin { animation: spin 1.2s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.state-text { font-size: 15px; font-weight: 600; color: var(--ink); }
.state-hint { font-size: 12.5px; color: var(--ink-3); margin-top: 6px; }

/* 领奖台 */
.podium {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 14px;
  margin-bottom: 22px;
  padding-top: 14px;
}
.podium-item {
  position: relative;
  flex: 1;
  max-width: 200px;
  text-align: center;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  padding: 18px 14px 0;
  box-shadow: var(--shadow-s);
  overflow: hidden;
  animation: rise .5s ease-out both;
}
@keyframes rise {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}
.podium-item.gold {
  background: linear-gradient(170deg, #FFFCF2, #FFF4D6);
  border-color: #F8E2AE;
  box-shadow: 0 10px 26px rgba(245, 158, 11, .16);
}
.podium-item.silver { background: linear-gradient(170deg, #FFFFFF, #F6F8FB); border-color: #E3E9F0; }
.podium-item.bronze { background: linear-gradient(170deg, #FFFFFF, #FDF6F0); border-color: #F2E2D3; }

.crown { position: absolute; top: 6px; left: 50%; transform: translateX(-50%); font-size: 18px; animation: bob 2s ease-in-out infinite; }
@keyframes bob { 0%, 100% { transform: translateX(-50%) translateY(0); } 50% { transform: translateX(-50%) translateY(-3px); } }

.podium-medal { font-size: 24px; line-height: 1; margin-bottom: 8px; }
.podium-avatar {
  width: 66px;
  height: 66px;
  margin: 0 auto 10px;
  border-radius: 50%;
  background: var(--line-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.podium-avatar.champion {
  width: 82px; height: 82px;
  background: linear-gradient(160deg, #FFF6DE, #FFE9AE);
  box-shadow: 0 0 0 3px rgba(245, 158, 11, .16);
}
.podium-name { font-size: 14px; font-weight: 700; color: var(--ink); margin-bottom: 4px; }
.podium-score { font-size: 20px; font-weight: 800; color: var(--ink); font-variant-numeric: tabular-nums; letter-spacing: -.02em; }
.podium-item.gold .podium-score { color: #B45309; }
.podium-score i { font-style: normal; font-size: 11px; font-weight: 600; color: var(--ink-3); margin-left: 2px; }
.podium-level { font-size: 11.5px; color: var(--ink-3); margin-top: 3px; padding-bottom: 14px; }
.podium-stand {
  height: 26px;
  line-height: 26px;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink-3);
  background: var(--line-soft);
  margin: 0 -14px;
}
.podium-item.gold .podium-stand { background: rgba(245, 158, 11, .16); color: #B45309; }

/* 单人 */
.solo {
  max-width: 260px;
  margin: 0 auto 22px;
  text-align: center;
  background: linear-gradient(170deg, #FFFCF2, #FFF4D6);
  border: 1px solid #F8E2AE;
  border-radius: var(--r-lg);
  padding: 26px 20px;
  position: relative;
  box-shadow: var(--shadow-s);
}
.solo-name { font-size: 16px; font-weight: 700; margin-top: 10px; }

/* 排名列表 */
.rank-list {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  overflow: hidden;
  box-shadow: var(--shadow-s);
}
.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 16px;
  border-bottom: 1px solid var(--line-soft);
  position: relative;
}
.rank-item:last-child { border-bottom: none; }
.rank-item:hover { background: #FCFAF9; }
.rank-number {
  width: 22px;
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-4);
  font-variant-numeric: tabular-nums;
}
.rank-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--line-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex: 0 0 34px;
}
.rank-info { width: 108px; min-width: 0; }
.rank-name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rank-level { font-size: 11px; color: var(--ink-3); }
.rank-score {
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  min-width: 52px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.rank-score i { font-style: normal; font-size: 10.5px; font-weight: 500; color: var(--ink-4); margin-left: 2px; }
.rank-bar {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: var(--line-soft);
  overflow: hidden;
  min-width: 60px;
}
.rank-bar-fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--brand), var(--brand-2));
  transition: width .4s ease;
}

@media (max-width: 768px) {
  .podium { gap: 8px; }
  .podium-item { padding: 14px 10px 0; border-radius: var(--r-md); }
  .podium-avatar { width: 52px; height: 52px; }
  .podium-avatar.champion { width: 64px; height: 64px; }
  .podium-score { font-size: 16px; }
  .podium-name { font-size: 12.5px; }
  .podium-medal { font-size: 20px; }
  .podium-stand { margin: 0 -10px; }
  .hint { display: none; }
  .rank-info { width: 78px; }
  .rank-bar { min-width: 40px; }
  .rank-item { gap: 9px; padding: 10px 12px; }
}
</style>
