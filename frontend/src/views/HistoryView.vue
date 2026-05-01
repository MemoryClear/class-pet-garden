<template>
  <div class="history-page">
    <div class="header-bar">
      <button class="back-btn" @click="router.push('/home')">← 返回</button>
      <h2>📜 加减分历史</h2>
    </div>
    <div class="container">
      <!-- 筛选 -->
      <div class="filter-bar">
        <select v-model="filterStudentId" class="filter-select">
          <option value="">全部学生</option>
          <option v-for="stu in appStore.students" :key="stu.id" :value="stu.id">{{ stu.name }}</option>
        </select>
      </div>
      <!-- 空状态 -->
      <div v-if="appStore.history.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-text">暂无加减分记录</div>
      </div>
      <!-- 历史列表 -->
      <div v-else class="history-list">
        <div v-for="item in appStore.history" :key="item.id" class="history-item" :class="{ revoked: item.revoked }">
          <div class="history-icon">{{ item.scoreItemIcon }}</div>
          <div class="history-info">
            <div class="history-name">
              {{ item.studentName }}
              <span v-if="item.revoked" class="revoked-badge">已撤销</span>
            </div>
            <div class="history-action">{{ item.scoreItemName }}</div>
          </div>
          <div class="history-point" :class="item.point > 0 ? 'positive' : 'negative'">
            {{ item.point > 0 ? '+' : '' }}{{ item.point }}
          </div>
          <div class="history-meta">
            <div class="history-time">{{ formatTime(item.createdAt) }}</div>
            <button
              v-if="!item.revoked"
              class="revoke-btn"
              @click="handleRevoke(item)"
              title="撤销此操作"
            >↩ 撤销</button>
            <div v-else class="revoked-time">撤销于 {{ formatTime(item.revokedAt) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 撤销确认弹窗 -->
    <div v-if="revokeTarget" class="modal-overlay" @click.self="revokeTarget = null">
      <div class="modal-card">
        <h3>确认撤销</h3>
        <p class="modal-desc">
          撤销 <strong>{{ revokeTarget.studentName }}</strong> 的
          <strong>{{ revokeTarget.scoreItemName }}</strong>
          ({{ revokeTarget.point > 0 ? '+' : '' }}{{ revokeTarget.point }}分)？
        </p>
        <p class="modal-warn">⚠️ 积分将反向调整，此操作仍会留痕</p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="revokeTarget = null">取消</button>
          <button class="btn-confirm" @click="confirmRevoke" :disabled="revoking">
            {{ revoking ? '撤销中...' : '确认撤销' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'
import { useRouter } from 'vue-router'
import api from '../api/index.js'

const appStore = useAppStore()
const router = useRouter()
const filterStudentId = ref('')
const revokeTarget = ref(null)
const revoking = ref(false)

watch(filterStudentId, async (id) => {
  await appStore.fetchHistory(id || null)
}, { immediate: false })

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getMonth()+1}/${d.getDate()} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

function handleRevoke(item) {
  revokeTarget.value = item
}

async function confirmRevoke() {
  if (!revokeTarget.value) return
  revoking.value = true
  try {
    await api.post(`/history/${revokeTarget.value.id}/revoke`)
    await appStore.fetchHistory(filterStudentId.value || null)
    await appStore.fetchStudents()
    revokeTarget.value = null
  } catch (e) {
    alert(e.response?.data?.error || '撤销失败')
  } finally {
    revoking.value = false
  }
}

onMounted(async () => {
  if (appStore.students.length === 0) await appStore.fetchStudents()
  await appStore.fetchHistory()
})
</script>

<style scoped>
.history-page { min-height:100vh; }
.header-bar { background:#fff; padding:16px 20px; display:flex; align-items:center; gap:16px; box-shadow:0 2px 8px rgba(0,0,0,0.06); margin-bottom:20px; }
.back-btn { background:#f0f0f0; border:none; padding:8px 16px; border-radius:8px; cursor:pointer; font-size:14px; }
h2 { font-size:18px; font-weight:600; color:#2d3748; }
.container { max-width:800px; margin:0 auto; padding:0 20px 40px; }
.filter-bar { margin-bottom:20px; }
.filter-select { padding:8px 12px; border:1px solid #ddd; border-radius:8px; font-size:14px; min-width:160px; }
.empty-state { text-align:center; padding:60px 20px; }
.empty-icon { width:80px; height:80px; background:#f7f3f0; border-radius:50%; display:flex; align-items:center; justify-content:center; margin:0 auto 16px; font-size:32px; }
.empty-text { font-size:16px; color:#666; }
.history-list { background:#fff; border-radius:16px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.06); }
.history-item { display:flex; align-items:center; gap:12px; padding:14px 16px; border-bottom:1px solid #f0f0f0; }
.history-item:last-child { border-bottom:none; }
.history-item.revoked { opacity:0.5; background:#fafafa; }
.history-icon { font-size:24px; }
.history-info { flex:1; }
.history-name { font-size:14px; font-weight:600; color:#2d3748; display:flex; align-items:center; gap:6px; }
.revoked-badge { font-size:11px; background:#fee2e2; color:#dc2626; padding:2px 6px; border-radius:4px; font-weight:500; }
.history-action { font-size:12px; color:#718096; margin-top:2px; }
.history-point { font-size:16px; font-weight:700; min-width:40px; text-align:center; }
.history-point.positive { color:#52c41a; }
.history-point.negative { color:#ef4444; }
.history-meta { text-align:right; min-width:80px; }
.history-time { font-size:12px; color:#999; }
.revoke-btn { margin-top:4px; background:none; border:1px solid #d9d9d9; padding:3px 8px; border-radius:4px; font-size:11px; color:#666; cursor:pointer; transition:all 0.2s; }
.revoke-btn:hover { border-color:#ff4d4f; color:#ff4d4f; background:#fff1f0; }
.revoked-time { font-size:11px; color:#bbb; margin-top:2px; }

/* Modal */
.modal-overlay { position:fixed; top:0; left:0; right:0; bottom:0; background:rgba(0,0,0,0.4); display:flex; align-items:center; justify-content:center; z-index:1000; }
.modal-card { background:#fff; border-radius:16px; padding:28px; width:90%; max-width:360px; }
.modal-card h3 { font-size:18px; font-weight:700; color:#1a1a1a; margin:0 0 16px; }
.modal-desc { font-size:14px; color:#4a4a4a; line-height:1.6; margin:0 0 12px; }
.modal-warn { font-size:13px; color:#d97706; margin:0 0 20px; }
.modal-actions { display:flex; gap:12px; justify-content:flex-end; }
.btn-cancel { padding:8px 20px; border:1px solid #ddd; border-radius:8px; background:#fff; font-size:14px; cursor:pointer; }
.btn-confirm { padding:8px 20px; border:none; border-radius:8px; background:#ff4d4f; color:#fff; font-size:14px; cursor:pointer; }
.btn-confirm:disabled { opacity:0.6; cursor:not-allowed; }
.btn-confirm:hover:not(:disabled) { background:#cf1322; }
</style>