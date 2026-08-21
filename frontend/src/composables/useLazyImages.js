// 共用的图片懒加载 composable
// 用法：
//   const { isVisible, registerImage, placeholderSrc } = useLazyImages()
//   <img :src="isVisible(id) ? realUrl : placeholderSrc" loading="lazy" decoding="async" :ref="el => registerImage(el, id)" />

import { onBeforeUnmount, ref } from 'vue'

let sharedObserver = null

function getObserver() {
  if (sharedObserver) return sharedObserver
  if (typeof IntersectionObserver === 'undefined') return null
  sharedObserver = new IntersectionObserver(
    entries => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue
        const cb = entry.target.__onVisible
        if (typeof cb === 'function') cb()
      }
    },
    { rootMargin: '200px 0px', threshold: 0.01 }
  )
  return sharedObserver
}

const PLACEHOLDER_SRC =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80"><rect width="80" height="80" fill="#f1f5f9"/></svg>'
  )

export function useLazyImages() {
  // 用 ref + reactive Map 触发 Vue 重渲染
  const visibleMap = ref(new Map()) // id (string) -> true

  function isVisible(id) {
    if (!getObserver()) return true // 浏览器不支持时直接显示
    return visibleMap.value.has(String(id))
  }

  function registerImage(el, id) {
    const observer = getObserver()
    if (!observer) return
    const key = String(id)
    if (!el) return
    el.dataset.lazyId = key
    el.__onVisible = () => {
      // 触发响应式更新：复制 Map 加新 key
      if (!visibleMap.value.has(key)) {
        const next = new Map(visibleMap.value)
        next.set(key, true)
        visibleMap.value = next
      }
      observer.unobserve(el)
    }
    observer.observe(el)
  }

  onBeforeUnmount(() => {
    // 元素被卸载，observer 自动断开
  })

  return {
    isVisible,
    registerImage,
    placeholderSrc: PLACEHOLDER_SRC,
  }
}