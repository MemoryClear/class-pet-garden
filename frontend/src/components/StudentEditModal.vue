<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-card">
      <div class="modal-header">
        <div class="modal-title">✏️ 编辑学生</div>
        <button class="modal-close" @click="emit('close')">×</button>
      </div>
      <div class="modal-body">
        <div class="field-row">
          <label>姓名</label>
          <input v-model="form.name" placeholder="学生姓名" maxlength="10" />
        </div>
        <div class="field-row">
          <label>学号</label>
          <div class="studentno-input">
            <span class="studentno-prefix">{{ studentNoPrefix }}</span>
            <input
              v-model="studentNoSuffix"
              placeholder="0001"
              maxlength="20"
              class="studentno-suffix"
            />
          </div>
        </div>
        <div class="field-tip">
          班级前缀「{{ studentNoPrefix }}」固定不可修改，只能调整后缀部分
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="emit('close')">取消</button>
        <button class="btn-save" @click="save" :disabled="saving">保存</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { studentApi } from '../api/index.js'
import { useAuthStore } from '../stores/auth.js'
import $confirm from '../composables/useConfirmModal.js'


const props = defineProps({
  student: { type: Object, required: true }
})
const emit = defineEmits(['close', 'updated'])
const authStore = useAuthStore()
const saving = ref(false)

const original = { ...(props.student || {}) }
const form = ref({ name: props.student?.name ?? '', studentNo: props.student?.studentNo ?? '' })

// 解析学号：取第一个 '-' 之前的部分为前缀，之后为后缀
const _rawSno = props.student?.studentNo || ''
const _dashIdx = _rawSno.indexOf('-')
const studentNoPrefix = _dashIdx >= 0 ? _rawSno.substring(0, _dashIdx + 1) : ''
const _initialSuffix = _dashIdx >= 0 ? _rawSno.substring(_dashIdx + 1) : ''
const studentNoSuffix = ref(_initialSuffix)

// 拼成完整学号用于提交
const fullStudentNo = computed(() => studentNoPrefix + studentNoSuffix.value.trim())

// 后端错误响应格式兼容多种 key：error / message / 字段错误
function parseError(e) {
  const data = e.response?.data
  if (!data) return e.message || '保存失败'
  if (typeof data === 'string') return data
  return data.error || data.message || data.msg || '保存失败'
}

function save() {
  saving.value = true
  const data = {}
  if (form.value.name && form.value.name.trim() && form.value.name !== original.name) {
    data.name = form.value.name.trim()
  }
  const trimmedSuffix = studentNoSuffix.value.trim()
  const newSno = fullStudentNo.value
  if (trimmedSuffix && newSno !== (original.studentNo || '')) {
    data.studentNo = newSno
  }
  if (Object.keys(data).length === 0) { emit('close'); return }

  studentApi.update(props.student.id, data)
    .then(() => { emit('updated', { ...props.student, ...data }); emit('close') })
    .catch(e => {
      $confirm.error('保存失败：' + parseError(e))
      saving.value = false
    })
}
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center; z-index: 2000;
}
.modal-card {
  background: #fff; border-radius: 16px; width: 360px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid #eee;
}
.modal-title { font-size: 18px; font-weight: 600; }
.modal-close { width: 28px; height: 28px; border: none; background: #f5f5f5; border-radius: 50%; font-size: 18px; cursor: pointer; }
.modal-body { padding: 20px; display: flex; flex-direction: column; gap: 14px; }
.field-row { display: flex; flex-direction: column; gap: 6px; }
.field-row label { font-size: 13px; color: #666; font-weight: 500; }
.field-row input {
  padding: 10px 12px; border: 1.5px solid #e0e0e0; border-radius: 8px;
  font-size: 15px; outline: none; transition: border-color 0.2s;
}
.field-row input:focus { border-color: #667eea; }
.field-tip { font-size: 12px; color: #999; margin-top: -8px; }

/* 学号输入框：前缀锁定 + 后缀可编辑 */
.studentno-input {
  display: flex;
  align-items: stretch;
  border: 1.5px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.2s;
}
.studentno-input:focus-within { border-color: #667eea; }
.studentno-prefix {
  padding: 10px 12px;
  background: #f5f5f5;
  color: #666;
  font-size: 15px;
  font-weight: 500;
  font-family: monospace;
  border-right: 1px solid #e0e0e0;
  user-select: none;
  white-space: nowrap;
}
.studentno-suffix {
  flex: 1;
  border: none !important;
  outline: none !important;
  padding: 10px 12px !important;
  font-size: 15px !important;
  font-family: monospace;
  background: transparent;
}
.studentno-suffix:focus { border: none !important; }
.modal-footer { display: flex; gap: 10px; padding: 12px 20px 16px; justify-content: flex-end; }
.btn-cancel { padding: 8px 18px; border: 1px solid #e0e0e0; background: #fff; border-radius: 8px; cursor: pointer; font-size: 14px; }
.btn-save { padding: 8px 20px; border: none; background: #667eea; color: white; border-radius: 8px; cursor: pointer; font-size: 14px; }
.btn-save:disabled { opacity: 0.6; }
</style>