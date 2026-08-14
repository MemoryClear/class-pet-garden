<template>
  <div class="board-page">
    <div class="board-header">
      <div class="brand">
        <span class="logo">🌸</span>
        <h1>公开看板</h1>
      </div>
      <router-link to="/" class="back-btn">返回登录</router-link>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="teachers.length === 0" class="empty-state">
      <div class="empty-icon">📭</div>
      <p>暂无班级添加到看板</p>
      <p class="hint">教师在「设置 → 班级信息」中开启「加入公开看板」后，看板会显示该班级</p>
    </div>

    <template v-else>
      <div class="class-tabs">
        <button
          v-for="t in teachers"
          :key="t.id"
          :class="['tab-btn', { active: selectedId === t.id }]"
          @click="selectTeacher(t.id)"
        >
          <span class="tab-name">{{t.className || '未命名班级'}}</span>
          <span v-if="t.systemName" class="tab-system">{{t.systemName}}</span>
        </button>
      </div>

      <div v-if="loadingLeaderboard" class="loading">加载排行榜...</div>
      <div v-else-if="leaderboard.length === 0" class="empty-state">
        <div class="empty-icon">🐣</div>
        <p>该班级暂无学生上榜</p>
      </div>
      <ol v-else class="rank-list">
        <li
          v-for="(s, idx) in leaderboard"
          :key="s.id"
          class="rank-item"
          @click="openStudent(s)"
        >
          <span :class="['rank-medal', `rank-${idx + 1}`]">{{idx + 1}}</span>
          <img v-if="s.petIcon" :src="s.petIcon" class="pet-icon" :alt="s.petName" @error="onPetIconError" />
          <span v-else class="pet-icon-placeholder">❓</span>
          <div class="rank-info">
            <div class="rank-name">{{s.name}}</div>
            <div class="rank-no">#{{s.studentNo}}</div>
          </div>
          <div class="rank-food">
            <span class="food-icon">🍱</span>
            <span class="food-num">{{s.food || 0}}</span>
          </div>
        </li>
      </ol>
    </template>

    <!-- 学生详情侧栏 -->
    <transition name="drawer">
      <div v-if="selectedStudent" class="drawer-overlay" @click.self="closeStudent">
        <div class="drawer">
          <button class="close-btn" @click="closeStudent">×</button>
          <div class="student-title">
            <img v-if="selectedStudent.pet?.icon" :src="selectedStudent.pet.icon" class="pet-big" :alt="selectedStudent.pet.name" />
            <span v-else class="pet-big-placeholder">❓</span>
            <div class="title-info">
              <h2>{{selectedStudent.name}}</h2>
              <div class="title-meta">
                <span>#{{selectedStudent.studentNo}}</span>
                <span>🍱 {{selectedStudent.food || 0}}</span>
                <span v-if="selectedStudent.pet?.name">｜ {{selectedStudent.pet.name}}</span>
              </div>
            </div>
          </div>

          <div v-if="loadingDetail" class="loading-small">加载中...</div>

          <div v-else class="records">
            <h3 class="section-title">📊 近期明细</h3>
            <div v-if="selectedStudent.scores?.length === 0" class="empty-small">暂无记录</div>
            <ul v-else class="record-list scrollable">
              <li v-for="r in selectedStudent.scores" :key="r.id" class="record-item">
                <span class="record-icon">{{r.icon || '📝'}}</span>
                <div class="record-content">
                  <div class="record-name">{{r.name}}</div>
                  <div class="record-time">{{formatTime(r.createdAt)}}</div>
                </div>
                <span :class="['record-point', r.point > 0 ? 'pos' : 'neg']">
                  {{r.point > 0 ? '+' : ''}}{{r.point}}
                </span>
              </li>
            </ul>

            <h3 class="section-title">🎁 礼物动向</h3>
            <div v-if="selectedStudent.gifts?.length === 0" class="empty-small">暂无礼物</div>
            <ul v-else class="record-list scrollable">
              <li v-for="g in selectedStudent.gifts" :key="g.id" class="record-item">
                <span class="record-icon">{{g.itemIcon || '🎁'}}</span>
                <div class="record-content">
                  <div class="record-name">{{g.itemName}}</div>
                  <div class="record-time">
                    <span v-if="g.giftToName">→ 赠送给 {{g.giftToName}}</span>
                    <span v-else-if="g.giftFromName">来自 {{g.giftFromName}}</span>
                    <span v-else>自己购买</span>
                    ・{{formatTime(g.createdAt)}}
                  </div>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const teachers = ref([])
const selectedId = ref(null)
const leaderboard = ref([])
const selectedStudent = ref(null)
const loading = ref(true)
const loadingLeaderboard = ref(false)
const loadingDetail = ref(false)

const formatTime = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  if (isNaN(d)) return iso
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const fetchTeachers = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/board/teachers')
    teachers.value = data || []
    if (teachers.value.length > 0) {
      selectTeacher(teachers.value[0].id)
    }
  } catch (e) {
    console.error('fetchTeachers failed', e)
    teachers.value = []
  } finally {
    loading.value = false
  }
}

const selectTeacher = async (id) => {
  selectedId.value = id
  loadingLeaderboard.value = true
  leaderboard.value = []
  try {
    const { data } = await axios.get(`/api/board/teachers/${id}/leaderboard`)
    leaderboard.value = data || []
  } catch (e) {
    console.error('fetch leaderboard failed', e)
    leaderboard.value = []
  } finally {
    loadingLeaderboard.value = false
  }
}

const openStudent = async (student) => {
  selectedStudent.value = student
  loadingDetail.value = true
  try {
    const { data } = await axios.get(`/api/board/teachers/${selectedId.value}/students/${student.id}/records`)
    selectedStudent.value = { ...student, ...data }
  } catch (e) {
    console.error('fetch student records failed', e)
    selectedStudent.value = {
      ...student,
      scores: [],
      gifts: [],
      error: '记录加载失败'
    }
  } finally {
    loadingDetail.value = false
  }
}

const closeStudent = () => {
  selectedStudent.value = null
}

const onPetIconError = (e) => {
  e.target.style.display = 'none'
}

onMounted(() => {
  fetchTeachers()
})
</script>

<style scoped>
.board-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff7ed 0%, #fef3c7 100%);
  padding: 24px 16px 80px;
  max-width: 720px;
  margin: 0 auto;
}

.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  font-size: 32px;
}

.board-header h1 {
  margin: 0;
  font-size: 24px;
  color: #9a3412;
}

.back-btn {
  background: #fff;
  color: #9a3412;
  padding: 8px 16px;
  border-radius: 999px;
  text-decoration: none;
  font-size: 14px;
  border: 1px solid #fdba74;
}

.loading, .empty-state {
  text-align: center;
  padding: 60px 16px;
  color: #78716c;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state .hint {
  font-size: 13px;
  color: #a8a29e;
  margin-top: 8px;
}

.class-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.tab-btn {
  background: #fff;
  border: 2px solid #fed7aa;
  border-radius: 14px;
  padding: 10px 16px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  min-width: 100px;
  transition: all 0.2s;
}

.tab-btn:hover {
  transform: translateY(-1px);
}

.tab-btn.active {
  background: linear-gradient(135deg, #fb923c, #f97316);
  border-color: #ea580c;
  color: #fff;
}

.tab-name {
  font-weight: 600;
  font-size: 14px;
}

.tab-system {
  font-size: 11px;
  opacity: 0.7;
}

.tab-btn.active .tab-system {
  opacity: 1;
}

.rank-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 14px;
  padding: 12px 16px;
  margin-bottom: 10px;
  box-shadow: 0 2px 6px rgba(251, 146, 60, 0.08);
  cursor: pointer;
  transition: transform 0.15s;
}

.rank-item:hover {
  transform: translateX(4px);
}

.rank-medal {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.rank-1 {
  background: linear-gradient(135deg, #fde68a, #f59e0b);
  color: #78350f;
}
.rank-2 {
  background: linear-gradient(135deg, #e5e7eb, #9ca3af);
  color: #1f2937;
}
.rank-3 {
  background: linear-gradient(135deg, #fed7aa, #c2410c);
  color: #fff;
}
.rank-medal:not(.rank-1):not(.rank-2):not(.rank-3) {
  background: #f5f5f4;
  color: #78716c;
}

.pet-icon, .pet-icon-placeholder {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: contain;
  background: #fff7ed;
  flex-shrink: 0;
}

.pet-icon-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-name {
  font-weight: 600;
  font-size: 15px;
  color: #292524;
}

.rank-no {
  font-size: 12px;
  color: #a8a29e;
  font-family: monospace;
}

.rank-food {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  color: #ea580c;
}

.food-num {
  font-size: 16px;
}

/* 侧栏 */
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: flex-end;
  z-index: 50;
}

.drawer {
  background: linear-gradient(180deg, #fff7ed 0%, #fef3c7 100%);
  width: 90%;
  max-width: 420px;
  height: 100%;
  /* 抽屉外层不滚动，由内层 .record-list.scrollable 各自独立滚动驱动 */
  overflow: hidden;
  padding: 24px 16px;
  position: relative;
  display: flex;
  flex-direction: column;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.8);
  font-size: 22px;
  cursor: pointer;
  color: #78716c;
}

.student-title {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px dashed #fed7aa;
  margin-bottom: 20px;
}

.pet-big, .pet-big-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: contain;
  background: #fff;
  flex-shrink: 0;
}

.pet-big-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.title-info h2 {
  margin: 0;
  font-size: 20px;
  color: #292524;
}

.title-meta {
  font-size: 13px;
  color: #78716c;
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.section-title {
  font-size: 15px;
  color: #9a3412;
  margin: 16px 0 10px;
  flex-shrink: 0;
}

/* 明细+礼物容器，抽屉 flex 子项，可伸缩 */
.records {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.record-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

/* 独立滚动区域：明细/礼物分别作为抽屉 flex 列中的独立滚动容器
   - flex: 1 + min-height: 0 互相瓜分剩余高度，剩余不足时各自独立滚动
   - 外层抽屉 overflow: hidden 避免双重滚动冲突
   - max-height 仅为手机极小屏留个兑底 */
.record-list.scrollable {
  flex: 1 1 0;
  min-height: 120px;
  overflow-y: auto;
  overflow-x: hidden;
  /* Firefox */
  scrollbar-width: thin;
  scrollbar-color: #fdba74 transparent;
}
/* WebKit 滚动条 */
.record-list.scrollable::-webkit-scrollbar {
  width: 6px;
}
.record-list.scrollable::-webkit-scrollbar-track {
  background: transparent;
}
.record-list.scrollable::-webkit-scrollbar-thumb {
  background: #fdba74;
  border-radius: 3px;
}
.record-list.scrollable::-webkit-scrollbar-thumb:hover {
  background: #f97316;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 6px;
}

.record-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.record-content {
  flex: 1;
  min-width: 0;
}

.record-name {
  font-size: 13px;
  font-weight: 500;
  color: #292524;
}

.record-time {
  font-size: 11px;
  color: #a8a29e;
  margin-top: 2px;
}

.record-point {
  font-weight: 700;
  font-size: 14px;
}

.record-point.pos {
  color: #16a34a;
}

.record-point.neg {
  color: #ea580c;
}

.empty-small {
  text-align: center;
  color: #a8a29e;
  padding: 16px;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 10px;
}

.loading-small {
  text-align: center;
  color: #a8a29e;
  padding: 24px;
  font-size: 13px;
}

/* 抽屉动画 */
.drawer-enter-active, .drawer-leave-active {
  transition: opacity 0.25s;
}
.drawer-enter-active .drawer,
.drawer-leave-active .drawer {
  transition: transform 0.25s ease;
}
.drawer-enter-from, .drawer-leave-to {
  opacity: 0;
}
.drawer-enter-from .drawer,
.drawer-leave-to .drawer {
  transform: translateX(100%);
}
</style>
