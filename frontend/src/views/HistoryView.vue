<template>
  <AppShell title="加减分历史" :subtitle="subtitle">
    <template #actions>
      <select v-model="filterStudentId" class="filter-select" aria-label="按学生筛选">
        <option value="">全部学生</option>
        <option v-for="stu in appStore.students" :key="stu.id" :value="stu.id">{{ stu.name }}</option>
      </select>
      <button class="btn-ghost" :disabled="appStore.historyLoading" @click="reload">
        {{ appStore.historyLoading ? '刷新中…' : '🔄 刷新' }}
      </button>
    </template>

    <div class="list-wrap">
      <!-- 空状态 -->
      <div v-if="appStore.history.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-text">暂无加减分记录</div>
        <div class="empty-hint">回到宠物墙点一张卡片，加减分后就会出现在这里</div>
      </div>

      <!-- 历史列表 -->
      <div v-else class="history-list">
        <div
          v-for="item in appStore.history"
          :key="item.id"
          class="history-item"
          :class="{ revoked: item.revoked }"
        >
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
              :disabled="item.giftedOut"
              :title="item.giftedOut ? '该道具已赠送，无法撤销' : '撤销此操作'"
              @click="handleRevoke(item)"
            >↩ 撤销</button>
            <div v-else class="revoked-time">撤销于 {{ formatTime(item.revokedAt) }}</div>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div v-if="appStore.historyHasMore || appStore.historyLoading" class="load-more">
        <button v-if="appStore.historyHasMore" class="load-more-btn" :disabled="appStore.historyLoading" @click="loadMore">
          {{ appStore.historyLoading ? '加载中…' : '加载更多' }}
        </button>
        <span v-else class="load-more-end">— 已加载全部 —</span>
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
  </AppShell>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useAppStore } from '../stores/app.js'
import AppShell from '../components/layout/AppShell.vue'
import api from '../api/index.js'
import $confirm from '../composables/useConfirmModal.js'

const appStore = useAppStore()
const filterStudentId = ref('')
const revokeTarget = ref(null)
const revoking = ref(false)

const subtitle = computed(() => {
  const n = appStore.history.length
  if (!n) return '每一次加减分都会留痕'
  return appStore.historyHasMore ? `已加载 ${n} 条 · 还有更多` : `共 ${n} 条记录`
})

watch(filterStudentId, async (id) => {
  await appStore.fetchHistory(id || null)
}, { immediate: false })

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function reload() {
  return appStore.fetchHistory(filterStudentId.value || null)
}

function handleRevoke(item) {
  revokeTarget.value = item
}

async function confirmRevoke() {
  if (!revokeTarget.value) return
  // 二次拦截：如果已赠送，提示但不进入 API 调用
  if (revokeTarget.value.giftedOut) {
    $confirm.error('该道具已赠送，无法撤销')
    revokeTarget.value = null
    return
  }
  revoking.value = true
  try {
    await api.post(`/history/${revokeTarget.value.id}/revoke`)
    await appStore.fetchHistory(filterStudentId.value || null)
    await appStore.fetchStudents()
    revokeTarget.value = null
  } catch (e) {
    $confirm.error(e.response?.data?.error || '撤销失败')
  } finally {
    revoking.value = false
  }
}

onMounted(async () => {
  if (appStore.students.length === 0) await appStore.fetchStudents()
  await appStore.fetchHistory()
})

async function loadMore() {
  if (!appStore.historyHasMore || appStore.historyLoading) return
  await appStore.fetchHistory(filterStudentId.value || null, null, null, { reset: false })
}
</script>

<style scoped>
.list-wrap { max-width: 860px; margin: 0 auto; }

.filter-select {
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  background: var(--surface);
  font-size: 13px;
  font-family: inherit;
  color: var(--ink);
  min-width: 140px;
}
.filter-select:focus { outline: 0; border-color: var(--brand); box-shadow: 0 0 0 3px var(--brand-soft); }

.empty-state {
  text-align: center;
  padding: 56px 20px;
  background: var(--surface);
  border: 1px dashed var(--line);
  border-radius: var(--r-lg);
}
.empty-icon {
  width: 72px; height: 72px;
  margin: 0 auto 14px;
  border-radius: 50%;
  background: var(--line-soft);
  display: flex; align-items: center; justify-content: center;
  font-size: 30px;
}
.empty-text { font-size: 15px; font-weight: 600; color: var(--ink); margin-bottom: 6px; }
.empty-hint { font-size: 12.5px; color: var(--ink-3); }

.history-list {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  overflow: hidden;
  box-shadow: var(--shadow-s);
}
.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 16px;
  border-bottom: 1px solid var(--line-soft);
}
.history-item:last-child { border-bottom: none; }
.history-item.revoked { opacity: .55; background: #FAF8F7; }
.history-icon { font-size: 22px; width: 28px; text-align: center; }
.history-info { flex: 1; min-width: 0; }
.history-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  display: flex;
  align-items: center;
  gap: 6px;
}
.revoked-badge {
  font-size: 11px;
  background: #FEE2E2;
  color: #DC2626;
  padding: 1px 6px;
  border-radius: 5px;
  font-weight: 500;
}
.history-action { font-size: 12px; color: var(--ink-3); margin-top: 2px; }
.history-point { font-size: 16px; font-weight: 700; min-width: 44px; text-align: center; font-variant-numeric: tabular-nums; }
.history-point.positive { color: var(--good); }
.history-point.negative { color: var(--bad); }
.history-meta { text-align: right; min-width: 84px; }
.history-time { font-size: 12px; color: var(--ink-3); }
.revoke-btn {
  margin-top: 5px;
  background: none;
  border: 1px solid var(--line);
  padding: 3px 9px;
  border-radius: 6px;
  font-size: 11px;
  color: var(--ink-2);
  cursor: pointer;
  transition: .18s;
  touch-action: manipulation;
}
.revoke-btn:hover { border-color: #FCA5A5; color: var(--bad); background: #FEF2F2; }
.revoke-btn:disabled { border-color: var(--line); color: var(--ink-4); background: var(--line-soft); cursor: not-allowed; }
.revoked-time { font-size: 11px; color: var(--ink-4); margin-top: 3px; }

.load-more { text-align: center; margin-top: 16px; }
.load-more-btn {
  background: var(--surface);
  border: 1px solid var(--brand-soft-2);
  color: var(--brand-deep);
  padding: 9px 26px;
  border-radius: var(--r-sm);
  font-size: 13.5px;
  cursor: pointer;
  transition: .18s;
}
.load-more-btn:hover:not(:disabled) { background: var(--brand-soft); }
.load-more-btn:disabled { opacity: .6; cursor: not-allowed; }
.load-more-end { color: var(--ink-4); font-size: 12px; }

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(20, 22, 30, .45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}
.modal-card {
  background: var(--surface);
  border-radius: var(--r-lg);
  padding: 26px;
  width: 100%;
  max-width: 360px;
  box-shadow: var(--shadow-m);
}
.modal-card h3 { font-size: 17px; font-weight: 700; color: var(--ink); margin: 0 0 14px; }
.modal-desc { font-size: 13.5px; color: var(--ink-2); line-height: 1.65; margin: 0 0 10px; }
.modal-warn { font-size: 12.5px; color: var(--warn); margin: 0 0 20px; }
.modal-actions { display: flex; gap: 10px; justify-content: flex-end; }
.btn-cancel {
  padding: 9px 18px;
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  background: var(--surface);
  font-size: 13.5px;
  color: var(--ink-2);
  cursor: pointer;
}
.btn-confirm {
  padding: 9px 18px;
  border: none;
  border-radius: var(--r-sm);
  background: var(--bad);
  color: #fff;
  font-size: 13.5px;
  cursor: pointer;
}
.btn-confirm:disabled { opacity: .6; cursor: not-allowed; }
.btn-confirm:hover:not(:disabled) { background: #DC2626; }

@media (max-width: 768px) {
  .history-item { gap: 10px; padding: 12px 13px; flex-wrap: wrap; }
  .history-meta { text-align: right; }
  .history-point { min-width: 36px; }
  .filter-select { min-width: 0; width: 118px; height: 34px; font-size: 12.5px; flex: 1; }
  .btn-ghost { padding: 7px 10px; font-size: 12.5px; }
}
</style>
