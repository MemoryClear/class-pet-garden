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
            <div class="level-item level-1">
              <div class="level-icon">🐣</div>
              <div class="level-info">
                <div class="level-name">Lv.1 幼崽期</div>
                <div class="level-range">0 - 9 分</div>
                <div class="level-desc">刚领养的小可爱，需要多多关爱</div>
              </div>
            </div>
            
            <div class="level-item level-2">
              <div class="level-icon">🌟</div>
              <div class="level-info">
                <div class="level-name">Lv.2 少年期</div>
                <div class="level-range">10 - 24 分</div>
                <div class="level-desc">开始散发光芒，出现星星装饰✨</div>
              </div>
            </div>
            
            <div class="level-item level-3">
              <div class="level-icon">💫</div>
              <div class="level-info">
                <div class="level-name">Lv.3 成年期</div>
                <div class="level-range">25 - 49 分</div>
                <div class="level-desc">成熟稳重，获得神秘光环💫</div>
              </div>
            </div>
            
            <div class="level-item level-4">
              <div class="level-icon">👑</div>
              <div class="level-info">
                <div class="level-name">Lv.4 传说期</div>
                <div class="level-range">50 分以上</div>
                <div class="level-desc">金色荣耀！拥有专属皇冠👑和光效</div>
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
const visible = ref(false)
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
  overflow-y: auto;
}
.guide-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
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
.guide-content { padding: 20px; }
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
.level-item.level-1 { background: #f0fff4; }
.level-item.level-2 { background: #ebf8ff; }
.level-item.level-3 { background: #faf5ff; }
.level-item.level-4 { background: #fffbe6; border: 2px solid #fbbf24; }

.level-icon { font-size: 32px; }
.level-info { flex: 1; }
.level-name { font-weight: 600; font-size: 15px; color: #2d3748; margin-bottom: 2px; }
.level-range { font-size: 12px; color: #888; margin-bottom: 4px; }
.level-desc { font-size: 13px; color: #666; }

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
