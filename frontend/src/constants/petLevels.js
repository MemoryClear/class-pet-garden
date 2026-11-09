/**
 * 宠物等级规则（唯一来源）
 * ------------------------------------------------------------
 * 分数 → 等级的换算、进度条百分比、等级标题、成长指南文案
 * 全部从这里读取，避免 PetCard / HomeView / LevelGuide 各写一份导致漂移。
 *
 * 阈值：Lv.1 起，达到 10 / 30 / 60 / 100 / 200 分依次升到 Lv.2 ~ Lv.6
 */

/** 每级起始分数，下标 0 对应 Lv.1 */
export const LEVEL_THRESHOLDS = Object.freeze([0, 10, 30, 60, 100, 200])

/** 最高等级 */
export const MAX_LEVEL = LEVEL_THRESHOLDS.length

/** 各等级展示信息（下标 0 对应 Lv.1） */
export const LEVELS = Object.freeze([
  { level: 1, name: '幼崽期', icon: '🐣', title: '', desc: '刚刚破壳的小可爱，需要多多关爱！🥚' },
  { level: 2, name: '少年期', icon: '🌱', title: '✦', desc: '开始散发光芒，出现星星装饰！✨' },
  { level: 3, name: '成长期', icon: '⚡', title: '★', desc: '实力提升，获得神秘光环！💫' },
  { level: 4, name: '精英期', icon: '💎', title: '★★', desc: '精英气质，晶体边框闪耀！💠' },
  { level: 5, name: '大师期', icon: '🔥', title: '★★★', desc: '大师风范，烈焰燃烧特效！' },
  { level: 6, name: '传说期', icon: '👑', title: '★★★★', desc: '传说降临，金色荣耀光环！🌟' }
])

/**
 * 分数 → 等级（1 ~ MAX_LEVEL）
 * 非法输入（null / undefined / NaN / 负数）一律按 0 分处理
 */
export function levelOf(food) {
  const score = Number(food)
  if (!Number.isFinite(score) || score <= 0) return 1
  for (let i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
    if (score >= LEVEL_THRESHOLDS[i]) return i + 1
  }
  return 1
}

/** 等级 → { min, max }，max 为 null 表示无上限 */
export function levelRange(level) {
  const lv = Math.min(Math.max(Math.trunc(level) || 1, 1), MAX_LEVEL)
  const min = LEVEL_THRESHOLDS[lv - 1]
  const max = lv < MAX_LEVEL ? LEVEL_THRESHOLDS[lv] - 1 : null
  return { min, max }
}

/** 等级 → 「10 - 29 分」/「200 分以上」 */
export function levelRangeText(level) {
  const { min, max } = levelRange(level)
  return max === null ? `${min} 分以上` : `${min} - ${max} 分`
}

/** 当前等级内的进度百分比（0 ~ 100），满级恒为 100 */
export function levelProgress(food) {
  const lv = levelOf(food)
  if (lv >= MAX_LEVEL) return 100
  const { min } = levelRange(lv)
  const span = LEVEL_THRESHOLDS[lv] - min
  const progress = (Number(food) || 0) - min
  if (span <= 0) return 100
  return Math.min(100, Math.max(0, (progress / span) * 100))
}

/** 等级 → 展示信息对象（越界时回落到 Lv.1） */
export function levelInfo(level) {
  const lv = Math.min(Math.max(Math.trunc(level) || 1, 1), MAX_LEVEL)
  return LEVELS[lv - 1]
}

/** 等级 → 称号星标（Lv.1 无称号） */
export function levelTitle(level) {
  return levelInfo(level).title
}

/** 距离下一级还差多少分，满级返回 0 */
export function foodToNextLevel(food) {
  const lv = levelOf(food)
  if (lv >= MAX_LEVEL) return 0
  return LEVEL_THRESHOLDS[lv] - (Number(food) || 0)
}
