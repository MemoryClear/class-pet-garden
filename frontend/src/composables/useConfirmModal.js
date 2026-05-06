/**
 * 全局 Confirm/Alert 模态框 composable
 * 使用方式:
 *   import $confirm from '../composables/useConfirmModal.js'
 *   $confirm.alert('操作成功')
 *   $confirm.confirm('确定删除吗？').then(ok => { if (ok) doSomething() })
 *   $confirm.error('出错了')
 *   $confirm.success('完成！')
 *   $confirm.warn('积分不足')
 */

// 全局实例（由 App.vue 启动时注入）
let _vm = null

export function setConfirmModal(vm) {
  _vm = vm
}

export default {
  get vm() { return _vm },

  // 通用 show
  show(opts) {
    if (!_vm) { console.warn('[useConfirmModal] 未挂载'); return Promise.resolve(false) }
    return _vm.show(opts)
  },

  // 确认框（返回 Promise<boolean>）
  confirm(message, opts = {}) {
    return this.show({
      title: opts.title || '确认操作',
      message,
      okText: opts.okText || '确定',
      cancelText: opts.cancelText || '取消',
      icon: opts.icon || '❓',
      ...opts,
    })
  },

  // 提示框（无需确认）
  alert(message, opts = {}) {
    return this.show({
      title: opts.title || '提示',
      message,
      okText: opts.okText || '我知道了',
      cancelText: null,
      icon: opts.icon || (opts.type === 'error' ? '❌' : opts.type === 'success' ? '✅' : '⚠️'),
      ...opts,
    })
  },

  // 错误提示
  error(message, opts = {}) {
    return this.show({
      title: opts.title || '操作失败',
      message,
      okText: opts.okText || '我知道了',
      cancelText: null,
      icon: '❌',
      ...opts,
    })
  },

  // 成功提示
  success(message, opts = {}) {
    return this.show({
      title: opts.title || '操作成功',
      message,
      okText: opts.okText || '太好了',
      cancelText: null,
      icon: '✅',
      ...opts,
    })
  },

  // 警告提示
  warn(message, opts = {}) {
    return this.show({
      title: opts.title || '温馨提示',
      message,
      okText: opts.okText || '我知道了',
      cancelText: null,
      icon: '⚠️',
      ...opts,
    })
  },
}
