<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="onCancel">
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-title">{{ title }}</div>
          <button class="modal-close" @click="onCancel">×</button>
        </div>
        <div class="modal-body">
          <div class="modal-icon" :class="iconClass">{{ icon }}</div>
          <div class="modal-message">{{ message }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="onCancel">{{ cancelText }}</button>
          <button class="btn-ok" :class="okClass" @click="onOk" :disabled="loading">
            {{ loading ? '处理中...' : okText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed } from 'vue'

const visible = ref(false)
const title = ref('确认操作')
const message = ref('')
const cancelText = ref('取消')
const okText = ref('确定')
const okClass = ref('btn-primary')
const icon = ref('❓')
const loading = ref(false)
let resolveFn = null

const iconClass = computed(() => {
  if (icon.value === '⚠️') return 'icon-warn'
  if (icon.value === '✅') return 'icon-success'
  return 'icon-info'
})

function show(opts) {
  return new Promise((resolve) => {
    title.value = opts.title || '确认操作'
    message.value = opts.message
    cancelText.value = opts.cancelText || '取消'
    okText.value = opts.okText || '确定'
    okClass.value = opts.danger ? 'btn-danger' : 'btn-primary'
    icon.value = opts.icon || '❓'
    loading.value = false
    resolveFn = resolve
    visible.value = true
  })
}

function onOk() {
  if (resolveFn) resolveFn(true)
  visible.value = false
}

function onCancel() {
  if (resolveFn) resolveFn(false)
  visible.value = false
}

// 暴露给外部调用
defineExpose({ show })
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 9999; padding: 20px;
}
.modal-card {
  background: #fff; border-radius: 20px;
  width: 100%; max-width: 400px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
  overflow: hidden;
  animation: popIn 0.2s ease;
}
@keyframes popIn {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px 0;
}
.modal-title { font-size: 18px; font-weight: 600; color: #1a1a1a; }
.modal-close {
  background: #f0f0f0; border: none;
  width: 30px; height: 30px; border-radius: 50%;
  font-size: 18px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  color: #666;
}
.modal-close:hover { background: #e0e0e0; }
.modal-body { padding: 24px; text-align: center; }
.modal-icon { font-size: 48px; margin-bottom: 16px; }
.icon-warn { color: #f59e0b; }
.icon-success { color: #52c41a; }
.icon-info { color: #3b82f6; }
.modal-message {
  font-size: 15px; color: #374151;
  line-height: 1.6;
  word-break: break-all;
}
.modal-footer { display: flex; gap: 12px; padding: 0 24px 24px; }
.modal-footer button {
  flex: 1; padding: 12px;
  border: none; border-radius: 10px;
  font-size: 15px; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
}
.btn-cancel {
  background: #f3f4f6; color: #374151;
}
.btn-cancel:hover { background: #e5e7eb; }
.btn-primary {
  background: linear-gradient(90deg, #f04e98 0%, #ed266e 100%);
  color: #fff;
}
.btn-primary:hover { opacity: 0.9; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-danger {
  background: linear-gradient(90deg, #ef4444 0%, #dc2626 100%);
  color: #fff;
}
.btn-danger:hover { opacity: 0.9; }
</style>
