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
          <input
            v-if="input"
            ref="inputEl"
            v-model="inputValue"
            class="modal-input"
            :type="inputType"
            :placeholder="inputPlaceholder"
            @keyup.enter="onOk"
          />
          <div v-if="inputHint" class="modal-input-hint">{{ inputHint }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="onCancel">{{ cancelText }}</button>
          <button class="btn-ok" :class="okClass" @click="onOk" :disabled="loading || (inputRequired && !inputValue)">
            {{ loading ? '处理中...' : okText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'

const visible = ref(false)
const title = ref('确认操作')
const message = ref('')
const cancelText = ref('取消')
const okText = ref('确定')
const okClass = ref('btn-primary')
const icon = ref('❓')
const loading = ref(false)
// 输入框相关
const input = ref(false)
const inputType = ref('text')
const inputValue = ref('')
const inputPlaceholder = ref('')
const inputHint = ref('')
const inputRequired = ref(false)
const inputEl = ref(null)
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
    input.value = !!opts.input
    inputType.value = opts.inputType || 'text'
    inputValue.value = opts.inputValue || ''
    inputPlaceholder.value = opts.inputPlaceholder || ''
    inputHint.value = opts.inputHint || ''
    inputRequired.value = !!opts.inputRequired
    resolveFn = resolve
    visible.value = true
    if (input.value) {
      nextTick(() => { inputEl.value?.focus() })
    }
  })
}

function onOk() {
  if (resolveFn) resolveFn(input.value ? inputValue.value : true)
  visible.value = false
}

function onCancel() {
  if (resolveFn) resolveFn(input.value ? null : false)
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
  padding: 18px 22px 0;
}
.modal-title {
  font-size: 17px; font-weight: 600; color: #2d3748;
}
.modal-close {
  background: none; border: none; cursor: pointer;
  font-size: 24px; color: #a0aec0; line-height: 1;
  padding: 0; width: 28px; height: 28px;
  border-radius: 8px;
}
.modal-close:hover { background: #f7fafc; color: #4a5568; }
.modal-body {
  padding: 18px 22px 8px;
  text-align: center;
}
.modal-icon {
  font-size: 36px;
  margin-bottom: 12px;
}
.modal-icon.icon-warn { color: #f59e0b; }
.modal-icon.icon-success { color: #10b981; }
.modal-icon.icon-info { color: #ff4d79; }
.modal-message {
  font-size: 15px;
  color: #4a5568;
  line-height: 1.6;
  text-align: center;
}
.modal-input {
  width: 100%;
  margin-top: 16px;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  text-align: left;
}
.modal-input:focus {
  border-color: #ff4d79;
  box-shadow: 0 0 0 3px rgba(255, 77, 121, 0.1);
}
.modal-input-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
  text-align: left;
}
.modal-footer {
  display: flex; gap: 10px;
  padding: 16px 22px 22px;
}
.btn-cancel, .btn-ok {
  flex: 1;
  padding: 11px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-cancel {
  background: #f7fafc;
  color: #4a5568;
}
.btn-cancel:hover { background: #edf2f7; }
.btn-primary {
  background: linear-gradient(135deg, #ff6b9d 0%, #ff4d79 100%);
  color: #fff;
}
.btn-primary:hover { opacity: 0.9; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-danger {
  background: linear-gradient(135deg, #ff7878 0%, #ff4d4d 100%);
  color: #fff;
}
.btn-danger:hover { opacity: 0.9; }
.btn-danger:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
