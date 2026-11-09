/**
 * 复制文本到剪贴板。
 *
 * 优先用 Clipboard API；但局域网 HTTP 部署时 `window.isSecureContext` 为 false，
 * navigator.clipboard 不可用，所以保留 textarea + execCommand 的兜底路径。
 *
 * @param {string} text
 * @returns {Promise<boolean>} 是否复制成功
 */
export async function copyText(text) {
  const value = String(text ?? '')
  if (!value) return false

  if (navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(value)
      return true
    } catch (e) {
      // 权限被拒或页面失焦，继续走兜底
    }
  }

  try {
    const ta = document.createElement('textarea')
    ta.value = value
    ta.setAttribute('readonly', '')
    ta.style.position = 'fixed'
    ta.style.top = '-1000px'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    ta.setSelectionRange(0, value.length)
    const ok = document.execCommand('copy')
    document.body.removeChild(ta)
    return ok
  } catch (e) {
    return false
  }
}

export default copyText
