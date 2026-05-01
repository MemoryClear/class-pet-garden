<template>
  <div class="pet-card" :class="[levelClass, { 'no-pet': !student.petId }]" @click="$emit('click', student)">
    <div class="level-badge" v-if="student.petId">Lv.{{ level }}</div>

    <div class="pet-top-deco" v-if="level >= 3 || headItems.length">
      <span v-if="level >= 4" class="deco-crown">👑</span>
      <span v-if="level === 3" class="deco-halo">💫</span>
      <!-- 头顶装饰 -->
      <span v-for="item in headItems" :key="item.id" class="equipped-deco head-deco" :title="item.name"> {{ item.icon }} </span>
    </div>

    <div class="pet-avatar-wrap">
      <!-- 左侧装饰 -->
      <span v-for="item in leftItems" :key="item.id" class="equipped-deco side-deco left-deco" :title="item.name">{{ item.icon }}</span>

      <div class="pet-avatar">
        <span v-if="level >= 2" class="deco deco-star1">✨</span>
        <span v-if="student.petIcon" class="pet-icon" :class="`size-${level}`">{{ student.petIcon }}</span>
        <span v-else class="pet-icon">❓</span>
        <span v-if="level >= 2" class="deco deco-star2">✨</span>
      </div>

      <!-- 右侧装饰 -->
      <span v-for="item in rightItems" :key="item.id" class="equipped-deco side-deco right-deco" :title="item.name">{{ item.icon }}</span>
    </div>

    <!-- 装饰装备栏 -->
    <div class="equipped-bar" v-if="allEquippedItems.length > 0">
      <div
        v-for="item in allEquippedItems"
        :key="item.id"
        class="equipped-chip-wrap"
        @click.stop="unequipTarget = unequipTarget?.id === item.id ? null : item"
      >
        <span class="equipped-chip" :title="item.name">{{ item.icon }}</span>
        <div class="popconfirm" v-if="unequipTarget?.id === item.id" @click.stop>
          <div class="popconfirm-arrow"></div>
          <div class="popconfirm-content">
            <div class="popconfirm-text">确定卸下 {{ item.name }}？</div>
            <div class="popconfirm-actions">
              <button class="popconfirm-btn cancel" @click="unequipTarget = null">取消</button>
              <button class="popconfirm-btn confirm" @click="confirmUnequip(item)">确定</button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="equipped-hint" v-else-if="student.petId">🏪 兑换装饰</div>

    <div class="pet-info">
      <div class="pet-name">
        {{ student.petName || '未领养' }}
        <span v-if="level >= 3" class="title-badge">{{ levelTitle }}</span>
      </div>
      <div class="student-name">{{ student.name }}</div>
    </div>

    <div class="pet-food">
      <span class="food-bar" :class="`bar-${level}`">
        <span class="food-fill" :style="{ width: foodPct + '%' }"></span>
        <span class="food-glow" v-if="level >= 4"></span>
      </span>
      <span class="food-emoji">🍖</span>
      <span class="food-val">{{ student.food ?? 0 }}</span>
      <span v-if="(student.petChangeCards ?? 0) > 0" class="pet-card-badge" title="宠物更换卡">🎫×{{ student.petChangeCards }}</span>
    </div>

    <div class="pet-actions" @click.stop>
      <button class="action-btn" @click="$emit('adopt', student)" title="领养/换宠物">
        {{ student.petId ? '🔄' : '➕' }}
      </button>
      <button class="action-btn exchange-btn" @click="$emit('exchange', student)" title="兑换商品">🏪</button>
      <button class="action-btn history-btn" @click="$emit('history', student)" title="查看明细">📋</button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useAppStore } from '../stores/app.js'

const props = defineProps({ student: { type: Object, required: true } })
const emit = defineEmits(['click', 'adopt', 'exchange', 'history'])
const appStore = useAppStore()

// 解析已装备的商品ID列表
const equippedIds = computed(() => {
  try {
    const json = props.student.equippedItems
    if (!json) return []
    return JSON.parse(json)
  } catch { return [] }
})

// 根据装备ID找到商品详情（从shopItems中）
const allEquippedItems = computed(() => {
  if (!appStore.shopItems.length) return []
  return equippedIds.value
    .map(id => appStore.shopItems.find(item => item.id === id))
    .filter(Boolean)
})

// 头顶装饰（头饰、围巾）
const headItems = computed(() =>
  allEquippedItems.value.filter(item =>
    ['🎀','👑','🎩','🌸','🎓','🧣'].includes(item.icon)
  )
)
// 左侧装饰（戒指、手表等小件）
const leftItems = computed(() =>
  allEquippedItems.value.filter(item =>
    ['💍','⌚','🕶️'].includes(item.icon)
  ).slice(0, 1)
)
// 右侧装饰
const rightItems = computed(() =>
  allEquippedItems.value.filter(item =>
    ['💍','⌚','🕶️'].includes(item.icon)
  ).slice(1, 2)
)

const level = computed(() => {
  const food = props.student.food ?? 0
  if (food >= 50) return 4
  if (food >= 25) return 3
  if (food >= 10) return 2
  return 1
})

const levelClass = computed(() => `level-${level.value}`)

const levelTitle = computed(() => {
  const titles = { 1: '', 2: '', 3: '★', 4: '★★★' }
  return titles[level.value] || ''
})

const foodPct = computed(() => {
  const food = props.student.food ?? 0
  const thresholds = [0, 10, 25, 50]
  const currentLevel = level.value
  if (currentLevel >= 4) return 100
  const currentMin = thresholds[currentLevel - 1]
  const nextMin = thresholds[currentLevel]
  const progress = food - currentMin
  const needed = nextMin - currentMin
  return Math.min(100, Math.max(0, (progress / needed) * 100))
})

// 卸下确认弹窗状态
const unequipTarget = ref(null)

// 确认卸下
const confirmUnequip = async (item) => {
  try {
    await appStore.unequipItem(props.student.id, item.id)
    unequipTarget.value = null
  } catch (e) {
    console.error('卸下失败', e)
  }
}

onMounted(async () => {
  if (!appStore.shopItems.length) {
    await appStore.fetchShopItems()
  }
})
</script>

<style scoped>
.pet-card {
  background: #fff;
  border-radius: 20px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}
.pet-card:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0,0,0,0.12); }
.pet-card.no-pet { background: linear-gradient(135deg, #fff 0%, #fff5f7 100%); border: 2px dashed #f0c0d0; }

.pet-card.level-2 { background: linear-gradient(135deg, #fff 0%, #f0f9ff 100%); }
.pet-card.level-3 { background: linear-gradient(135deg, #fff 0%, #faf5ff 100%); }
.pet-card.level-4 {
  background: linear-gradient(135deg, #fffbe6 0%, #fff1b8 100%);
  animation: legendary-glow 2s ease-in-out infinite;
}

@keyframes legendary-glow {
  0%, 100% { box-shadow: 0 4px 12px rgba(250, 200, 50, 0.3); }
  50% { box-shadow: 0 8px 25px rgba(250, 200, 50, 0.5); }
}

.level-badge {
  position: absolute;
  top: 10px; right: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white; font-size: 11px; font-weight: 700;
  padding: 3px 8px; border-radius: 10px;
}
.pet-card.level-4 .level-badge {
  background: linear-gradient(135deg, #ffd700 0%, #ff8c00 100%);
  animation: badge-pulse 1.5s ease-in-out infinite;
}
@keyframes badge-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.pet-top-deco {
  text-align: center;
  margin-bottom: -4px;
  min-height: 26px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 2px;
}
.deco-crown { font-size: 22px; animation: crown-bob 2s ease-in-out infinite; }
.deco-halo { font-size: 20px; opacity: 0.8; animation: twinkle 1.5s ease-in-out infinite; }
.head-deco { font-size: 18px; animation: float 3s ease-in-out infinite; }
@keyframes crown-bob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-2px); }
}

.pet-avatar-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 6px;
  gap: 0;
}

.pet-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.pet-icon {
  font-size: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s;
}
.pet-card:hover .pet-icon { transform: scale(1.1); }
.pet-icon.size-2 { font-size: 64px; }
.pet-icon.size-3 { font-size: 68px; filter: drop-shadow(0 2px 8px rgba(168,85,247,0.4)); }
.pet-icon.size-4 {
  font-size: 72px;
  filter: drop-shadow(0 4px 12px rgba(250,200,50,0.6));
  animation: icon-float 2s ease-in-out infinite;
}
@keyframes icon-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.deco { font-size: 14px; animation: twinkle 1.5s ease-in-out infinite; line-height: 1; }
.deco-star1 { animation-delay: 0s; }
.deco-star2 { animation-delay: 0.5s; }
@keyframes twinkle {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.85); }
}

/* 装备装饰样式 */
.equipped-deco {
  font-size: 18px;
  display: inline-flex;
  align-items: center;
  cursor: default;
  animation: float 3s ease-in-out infinite;
}
.side-deco { font-size: 16px; }
.left-deco { margin-right: 2px; }
.right-deco { margin-left: 2px; }
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

.pet-avatar-wrap {
  margin-bottom: 2px;
}

/* 装备栏 */
.equipped-bar {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
  min-height: 26px;
  padding: 3px 6px;
  background: #faf5ff;
  border-radius: 12px;
  margin-bottom: 6px;
}
.equipped-chip-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}
.equipped-chip {
  font-size: 16px;
  transition: transform 0.2s;
}
.equipped-chip-wrap:hover .equipped-chip { transform: scale(1.2); }

/* Popconfirm 弹窗 */
.popconfirm {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-top: 8px;
  z-index: 100;
}
.popconfirm-arrow {
  position: absolute;
  top: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-bottom: 6px solid #fff;
  filter: drop-shadow(0 -1px 1px rgba(0,0,0,0.1));
}
.popconfirm-content {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  padding: 10px 12px;
  min-width: 140px;
}
.popconfirm-text {
  font-size: 12px;
  color: #374151;
  margin-bottom: 8px;
  text-align: center;
}
.popconfirm-actions {
  display: flex;
  gap: 6px;
  justify-content: center;
}
.popconfirm-btn {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}
.popconfirm-btn.cancel {
  background: #f3f4f6;
  color: #6b7280;
}
.popconfirm-btn.cancel:hover { background: #e5e7eb; }
.popconfirm-btn.confirm {
  background: #ef4444;
  color: white;
}
.popconfirm-btn.confirm:hover { background: #dc2626; }
.equipped-hint {
  text-align: center;
  font-size: 11px;
  color: #c4a000;
  margin-bottom: 4px;
  opacity: 0.7;
  min-height: 20px;
}

.pet-info { text-align: center; margin-bottom: 10px; }
.pet-name { font-size: 15px; font-weight: 600; color: #2d3748; display: flex; align-items: center; justify-content: center; gap: 4px; }
.pet-card.level-4 .pet-name { color: #b7791f; }
.student-name { font-size: 12px; color: #999; margin-top: 2px; }
.title-badge { font-size: 10px; color: #a855f7; }

.pet-food { display: flex; align-items: center; gap: 6px; margin-bottom: 10px; }
.food-bar { flex: 1; height: 6px; background: #f0f0f0; border-radius: 3px; overflow: hidden; position: relative; }
.food-fill { height: 100%; background: linear-gradient(90deg, #ff9f69, #ffc168); border-radius: 3px; transition: width 0.3s; }
.food-bar.bar-2 .food-fill { background: linear-gradient(90deg, #60a5fa, #3b82f6); }
.food-bar.bar-3 .food-fill { background: linear-gradient(90deg, #a78bfa, #8b5cf6); }
.food-bar.bar-4 .food-fill { background: linear-gradient(90deg, #fbbf24, #f59e0b); }
.food-emoji { font-size: 12px; }
.food-val { font-size: 12px; font-weight: 600; color: #b87323; min-width: 20px; }
.pet-card.level-4 .food-val { color: #b7791f; }
.pet-card-badge {
  font-size: 10px;
  background: #e0f2fe;
  color: #0369a1;
  padding: 1px 4px;
  border-radius: 4px;
  margin-left: 4px;
}

.pet-actions { display: flex; justify-content: center; gap: 8px; }
.action-btn { background: #fff0f3; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; font-size: 14px; transition: all 0.2s; }
.action-btn:hover { background: #ffe0e6; transform: scale(1.1); }
.exchange-btn { background: #fef3c7; }
.exchange-btn:hover { background: #fde68a; }
</style>
