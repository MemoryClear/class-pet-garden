/**
 * 全局轻提示（toast）。
 *
 * 用法：toast('教师码已复制')
 * 复用同一个 DOM 节点，连续调用会重置计时器，不会堆叠。
 */
let el = null
let timer = null

export function toast(message, opts = {}) {
  if (typeof document === 'undefined') return
  if (!el) {
    el = document.createElement('div')
    el.className = 'app-toast'
    el.setAttribute('role', 'status')
    document.body.appendChild(el)
  }
  el.textContent = `${opts.icon || '✓'} ${message}`
  // 强制回流：连续触发时也能重播消失动画
  void el.offsetWidth
  el.classList.add('show')

  clearTimeout(timer)
  timer = setTimeout(() => el.classList.remove('show'), opts.duration || 1700)
}

export default toast
