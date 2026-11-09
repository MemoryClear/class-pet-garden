<template>
  <div class="level-guide" @click="visible = true">
    <span class="guide-icon">📊</span>
    <span class="guide-text">等级说明</span>
  </div>
  
  <Teleport to="body">
    <div v-if="visible" class="guide-overlay" @click.self="visible = false">
      <div class="guide-card">
        <div class="guide-header">
          <span class="guide-title">🐾 宠物成长指南</span>
          <button class="guide-close" @click="visible = false">×</button>
        </div>
        
        <div class="guide-content">
          <p class="guide-desc">通过课堂表现获得🍖分数，让宠物升级成长！</p>
          
          <div class="level-list">
            <div
              v-for="lv in LEVELS"
              :key="lv.level"
              class="level-item"
              :class="`level-${lv.level}`"
            >
              <div class="level-icon">{{ lv.icon }}</div>
              <div class="level-info">
                <div class="level-name">Lv.{{ lv.level }} {{ lv.name }}</div>
                <div class="level-range">{{ levelRangeText(lv.level) }}</div>
                <div class="level-desc">{{ lv.desc }}</div>
              </div>
            </div>
          </div>
          
          <div class="guide-tip">
            💡 提示：每次加分后宠物会自动升级，升级后外观会发生变化！
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import { LEVELS, levelRangeText } from '../constants/petLevels.js'

const visible = ref(false)

defineExpose({
  open: () => { visible.value = true },
  close: () => { visible.value = false }
})
</script>

<style scoped>
.level-guide {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: rgba(255,255,255,0.8);
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  transition: all 0.2s;
}
.level-guide:hover {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.guide-icon { font-size: 16px; }

.guide-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 20px;
}
.guide-card {
  background: #fff;
  border-radius: 20px;
  width: 100%;
  max-width: 420px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.guide-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.guide-title { font-size: 18px; font-weight: 600; }
.guide-close {
  background: #f0f0f0;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.guide-content { padding: 20px; overflow-y: auto; flex: 1; }
.guide-desc {
  text-align: center;
  color: #666;
  margin-bottom: 20px;
  font-size: 14px;
}

.level-list { display: flex; flex-direction: column; gap: 12px; }
.level-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border-radius: 12px;
  background: #f7fafc;
}
.level-item.level-1 { background: linear-gradient(135deg, #f0fff4, #dcfce7); }
.level-item.level-2 { background: linear-gradient(135deg, #ebf8ff, #dbeafe); }
.level-item.level-3 { background: linear-gradient(135deg, #faf5ff, #ede9fe); }
.level-item.level-4 { background: linear-gradient(135deg, #eff6ff, #dbeafe); border-left: 3px solid #60a5fa; }
.level-item.level-5 { background: linear-gradient(135deg, #fff7ed, #ffedd5); border-left: 3px solid #f97316; animation: master-shine 3s ease-in-out infinite; }
.level-item.level-6 { background: linear-gradient(135deg, #fffbeb, #fef3c7); border: 2px solid #fbbf24; animation: legendary-shine 2s ease-in-out infinite; }

.level-icon { font-size: 32px; }
.level-info { flex: 1; }
.level-name { font-weight: 600; font-size: 15px; color: #2d3748; margin-bottom: 2px; }
.level-range { font-size: 12px; color: #888; margin-bottom: 4px; }
.level-desc { font-size: 13px; color: #666; }

@media (max-width: 640px) {
  .guide-overlay { align-items: flex-end; padding: 0; }
  .guide-card {
    max-width: none;
    width: 100%;
    max-height: 86dvh;
    border-radius: 22px 22px 0 0;
    animation: sheet-up .24s ease-out;
  }
  .guide-close { width: 40px; height: 40px; }
  .guide-content { padding: 16px 16px calc(20px + env(safe-area-inset-bottom)); }
  .level-item { padding: 12px; }
}
@keyframes sheet-up {
  from { transform: translateY(16px); opacity: .6; }
  to { transform: translateY(0); opacity: 1; }
}

.guide-tip {
  margin-top: 16px;
  padding: 12px;
  background: #fff7ed;
  border-radius: 8px;
  font-size: 13px;
  color: #92400e;
  text-align: center;
}
</style>
