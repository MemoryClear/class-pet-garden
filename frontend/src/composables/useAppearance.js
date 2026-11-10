/**
 * 全局外观状态。
 *
 * 数据流：
 *   - 未登录（登录页）：用本机选择 `cp_guest_mode`，默认亮色；
 *     登录后这条选择不再生效（由下面的账号/班级设置接管）。
 *   - 教师端：外观来自账号设置 `teachers.theme_mode`（设置页 / 侧栏切换都会写回后端），
 *     换设备、换浏览器都保持一致。
 *   - 学生端：默认跟随**所属教师**的设置（学生没有设置页）；
 *     但学生可以在自己的设备上手动切换一次，选择存在 localStorage `cp_student_mode`，
 *     之后这台设备就按学生的选择走，不会被老师那边的改动覆盖。
 *
 * App.vue 会把最终结果写到 <html data-mode>，同时同步到 currentMode，
 * 供页面（如学生端顶栏的开关）读取当前状态。
 */
import { ref } from 'vue'

const STUDENT_KEY = 'cp_student_mode'
const GUEST_KEY = 'cp_guest_mode'

/** '' | 'light' | 'dark' —— 学生本机手动选择 */
export const studentMode = ref(localStorage.getItem(STUDENT_KEY) || '')

/** 登录页（未登录）手动选的外观，默认跟亮色 */
export const guestMode = ref(localStorage.getItem(GUEST_KEY) || '')

/** 当前实际生效的模式，由 App.vue 维护 */
export const currentMode = ref('light')

export function setGuestMode(m) {
  guestMode.value = m === 'dark' ? 'dark' : 'light'
  localStorage.setItem(GUEST_KEY, guestMode.value)
}

export function setStudentMode(m) {
  studentMode.value = m === 'dark' ? 'dark' : 'light'
  localStorage.setItem(STUDENT_KEY, studentMode.value)
}

export function clearStudentMode() {
  studentMode.value = ''
  localStorage.removeItem(STUDENT_KEY)
}
