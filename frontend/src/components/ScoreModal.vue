<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-card">
      <div class="modal-header">
        <div class="modal-title">{{ student.petIcon }} {{ student.name }}</div>
        <button class="modal-close" @click="$emit('close')">×</button>
      </div>

      <!-- 倍数选择器 -->
      <div class="multiplier-bar">
        <span class="multiplier-label">倍数：</span>
        <button
          v-for="m in [1, 2, 3, 5]"
          :key="m"
          :class="['mult-btn', { active: multiplier === m && !customMode }]"
          @click="multiplier = m; customMode = false"
        >×{{ m }}</button>
        <!-- 自定义输入 -->
        <button
          :class="['mult-btn', 'custom-toggle', { active: customMode }]"
          @click="toggleCustom"
        >自定义</button>
        <input
          v-if="customMode"
          type="number"
          v-model.number="customMultiplier"
          min="1"
          max="100"
          class="custom-input"
          placeholder="N"
          @input="onCustomInput"
        />
      </div>

      <div class="score-grid">
        <div v-for="item in appStore.scoreItems" :key="item.id"
             class="score-btn"
             :class="{ positive: item.point > 0, negative: item.point < 0 }"
             @click="applyScore(item)">
          <div class="score-emoji">{{ item.icon }}</div>
          <div class="score-name">{{ item.name }}</div>
          <div class="score-pt" :class="item.point > 0 ? 'pos' : 'neg'">
            {{ item.point > 0 ? '+' : '' }}{{ item.point * effectiveMultiplier }}
            <span v-if="effectiveMultiplier > 1" class="mult-hint">(×{{ effectiveMultiplier }})</span>
          </div>
        </div>
        <div v-if="appStore.scoreItems.length === 0" class="empty-tip">
          暂无评分项，请先去设置添加
        </div>
      </div>
    </div>

    <!-- 成功提示 -->
    <div v-if="successMsg" class="success-toast" @click="successMsg = null">
      ✅ {{ successMsg }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAppStore } from '../stores/app.js'
const props = defineProps({ student: { type: Object, required: true } })
defineEmits(['close'])
const appStore = useAppStore()

const multiplier = ref(1)
const customMode = ref(false)
const customMultiplier = ref(1)

const effectiveMultiplier = computed(() => {
  return customMode.value ? Math.max(1, Math.min(100, customMultiplier.value || 1)) : multiplier.value
})

function toggleCustom() {
  customMode.value = !customMode.value
  if (customMode.value) {
    customMultiplier.value = multiplier.value
  }
}

function onCustomInput() {
  // 限制范围 1-100
  if (customMultiplier.value < 1) customMultiplier.value = 1
  if (customMultiplier.value > 100) customMultiplier.value = 100
}

const successMsg = ref(null)

async function applyScore(item) {
  try {
    const mult = effectiveMultiplier.value
    const totalPoint = item.point * mult
    // 调用多次（后端每次加 item.point）
    for (let i = 0; i < mult; i++) {
      await appStore.applyScore(props.student.id, item.id)
    }
    // 显示成功提示
    const action = item.point > 0 ? '加分' : '减分'
    successMsg.value = `${item.name} ${action} ${Math.abs(totalPoint)} 分`
    setTimeout(() => { successMsg.value = null }, 2000)
  } catch (e) {
    console.error('评分失败', e)
  }
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 999; padding: 20px; }
.modal-card { background: #fff; border-radius: 20px; width: 100%; max-width: 480px; max-height: 80vh; overflow-y: auto; }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 20px; border-bottom: 1px solid #f0f0f0; }
.modal-title { font-size: 18px; font-weight: 600; }
.modal-close { background: #f0f0f0; border: none; width: 32px; height: 32px; border-radius: 50%; font-size: 20px; cursor: pointer; display: flex; align-items: center; justify-content: center; }

.multiplier-bar {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 20px; border-bottom: 1px solid #f0f0f0;
  background: #fafafa; flex-wrap: wrap;
}
.multiplier-label { font-size: 14px; color: #666; }
.mult-btn {
  background: #fff; border: 1px solid #ddd;
  padding: 4px 12px; border-radius: 8px;
  font-size: 13px; cursor: pointer;
  transition: all 0.2s;
}
.mult-btn:hover { border-color: #ff4d79; }
.mult-btn.active {
  background: #ff4d79; color: #fff;
  border-color: #ff4d79;
}
.custom-toggle {
  background: #fff0f3; border-color: #ffb3c1;
}
.custom-toggle.active {
  background: #ff4d79; color: #fff; border-color: #ff4d79;
}
.custom-input {
  width: 60px; padding: 4px 8px;
  border: 1px solid #ddd; border-radius: 8px;
  font-size: 13px; text-align: center;
}
.custom-input:focus {
  outline: none; border-color: #ff4d79;
}

.score-grid { padding: 16px; display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 12px; }
.score-btn { background: #f7fafc; border-radius: 12px; padding: 14px 8px; text-align: center; cursor: pointer; border: 2px solid transparent; transition: all 0.2s; }
.score-btn:hover { transform: scale(1.05); }
.score-btn.positive { background: #f0fff4; }
.score-btn.negative { background: #fff5f5; }
.score-emoji { font-size: 28px; margin-bottom: 4px; }
.score-name { font-size: 12px; color: #2d3748; margin-bottom: 2px; }
.score-pt { font-size: 14px; font-weight: 700; }
.score-pt.pos { color: #52c41a; }
.score-pt.neg { color: #ef4444; }
.mult-hint { font-size: 11px; opacity: 0.7; font-weight: 400; }
.empty-tip { grid-column: 1/-1; text-align: center; padding: 20px; color: #999; font-size: 14px; }

.success-toast {
  position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
  background: #52c41a; color: #fff;
  padding: 12px 24px; border-radius: 12px;
  font-size: 15px; font-weight: 500;
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.3);
  z-index: 9999; cursor: pointer;
  animation: slideDown 0.3s ease;
}
@keyframes slideDown {
  from { top: -20px; opacity: 0; }
  to { top: 20px; opacity: 1; }
}
</style>
