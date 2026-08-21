<template>
  <img v-if="isPath" :src="isVisible(uid) ? icon : placeholderSrc" :alt="alt" class="pet-icon-img" :class="{ 'pet-icon-img--loaded': isVisible(uid) }" :style="imgStyle" loading="lazy" decoding="async" :ref="el => registerImage(el, uid)" @error="onError" />
  <span v-else :style="spanStyle">{{ icon || fallback }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { useLazyImages } from '../composables/useLazyImages.js'

const { isVisible, registerImage, placeholderSrc } = useLazyImages()

// 每个实例唯一 uid（用于 IntersectionObserver 跟踪）
const uid = Math.random().toString(36).slice(2, 10) + Math.random().toString(36).slice(2, 10)

const props = defineProps({
  icon: { type: String, default: '' },
  alt: { type: String, default: '' },
  size: { type: [Number, String], default: 40 },
  fallback: { type: String, default: '❓' }
})

const isPath = computed(() => props.icon && props.icon.startsWith('/'))

const normalizedSize = computed(() => {
  if (typeof props.size === 'number') return props.size + 'px'
  if (typeof props.size === 'string') {
    const t = props.size.trim()
    if (/^\d+$/.test(t)) return t + 'px'
    return t
  }
  return '40px'
})

const imgStyle = computed(() => ({
  width: normalizedSize.value,
  height: normalizedSize.value,
  objectFit: 'contain'
}))

const spanStyle = computed(() => {
  const num = parseInt(normalizedSize.value)
  return {
    fontSize: (isNaN(num) ? 34 : num * 0.85) + 'px',
    lineHeight: '1'
  }
})

function onError(e) {
  e.target.style.display = 'none'
  e.target.nextElementSibling && (e.target.nextElementSibling.style.display = '')
}
</script>

<style scoped>
.pet-icon-img {
  display: inline-block;
  vertical-align: middle;
  image-rendering: auto;
  transition: opacity 0.2s ease;
  opacity: 0;
}
.pet-icon-img--loaded {
  opacity: 1;
}
</style>