/**
 * 生成"发给班级群"的学生登录说明。
 *
 * 学生登录只需要三样东西：地址、教师码、班级名；
 * 学号只填 S0001 这一段，班级号由下拉框决定，不用手输。
 */
export function buildClassInvite({ systemName, teacherCode, className, classCode }) {
  return [
    `【${systemName || '课堂宠物园'}】学生登录`,
    `地址：${location.origin}`,
    `教师码：${teacherCode || '（向老师索取）'}`,
    `班级：${className || '—'}`,
    `学号：S0001（只填这一段，不用输入 ${classCode || '班级号'}）`
  ].join('\n')
}

export default buildClassInvite
