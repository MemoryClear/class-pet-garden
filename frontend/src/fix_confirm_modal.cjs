/**
 * fix_confirm_modal.cjs
 * 将所有 alert / confirm 替换为 $confirm 全局模态框
 *
 * alert('msg')              → $confirm.alert('msg', {type:'error'})
 * alert('msg', ...)         → $confirm.alert('msg')
 * confirm('msg')            → $confirm.confirm('msg')
 * if (!confirm(...)) return → $confirm.confirm(...).then(ok => { if (!ok) return })
 *
 * PetSelectModal.vue: confirm() 函数重命名为 handleConfirm()（避免与全局冲突）
 */

const fs = require('fs')
const path = require('path')

// 替换规则：{ file: { old → new } }
const replacements = {

  // ── PetSelectModal.vue ──────────────────────────────────────────────────
  'components/PetSelectModal.vue': [
    // 1. 函数名冲突：async function confirm() → handleConfirm()
    { re: /async function confirm\(\)/g, newStr: 'async function handleConfirm()' },
    // 2. 调用处：await confirm() → await handleConfirm()
    { re: /await confirm\(\)/g, newStr: 'await handleConfirm()' },
    // 3. alert('请先到小卖部购买宠物更换卡')
    { re: /alert\('请先到小卖部购买宠物更换卡'\)/g,
      newStr: "$confirm.alert('请先到小卖部购买宠物更换卡', {type:'warn', title:'提示'})" },
    // 4. alert(msg)
    { re: /alert\(msg\)/g,
      newStr: "$confirm.error(msg)" },
  ],

  // ── StudentEditModal.vue ─────────────────────────────────────────────────
  'components/StudentEditModal.vue': [
    // alert('保存失败: ' + ...)
    { re: /alert\('保存失败: ' \+ \(e\.response\?\.data\?\.message \|\| e\.message\)\)/g,
      newStr: "$confirm.error('保存失败: ' + (e.response?.data?.message || e.message))" },
  ],

  // ── ClassroomView.vue ───────────────────────────────────────────────────
  'views/ClassroomView.vue': [
    // if (!confirm(`确定删除...`)) return
    { re: /if \(!confirm\(`确定删除《\$\{poem\.title\}》吗？`\)\) return/g,
      newStr: "if (!await $confirm.confirm(`确定删除《${poem.title}》吗？`)) return" },
    // alert(e.response?.data?.error || '删除失败')
    { re: /alert\(e\.response\?\.data\?\.error \|\| '删除失败'\)/g,
      newStr: "$confirm.error(e.response?.data?.error || '删除失败')" },
    // if (!mathConfig.operations.length) return alert('请选择运算')
    { re: /if \(!mathConfig\.operations\.length\) return alert\('请选择运算'\)/g,
      newStr: "if (!mathConfig.operations.length) { $confirm.alert('请选择运算'); return }" },
  ],

  // ── HistoryView.vue ─────────────────────────────────────────────────────
  'views/HistoryView.vue': [
    // alert(e.response?.data?.error || '撤销失败')
    { re: /alert\(e\.response\?\.data\?\.error \|\| '撤销失败'\)/g,
      newStr: "$confirm.error(e.response?.data?.error || '撤销失败')" },
  ],

  // ── SettingsView.vue ────────────────────────────────────────────────────
  'views/SettingsView.vue': [
    // if (!confirm('确认删除？')) return
    { re: /if \(!confirm\('确认删除？'\)\) return/g,
      newStr: "if (!await $confirm.confirm('确认删除？')) return" },
  ],

  // ── ShopView.vue ────────────────────────────────────────────────────────
  'views/ShopView.vue': [
    // alert('保存失败: ' + ...)
    { re: /alert\('保存失败: ' \+ \(e\.response\?\.data\?\.message \|\| e\.message\)\)/g,
      newStr: "$confirm.error('保存失败: ' + (e.response?.data?.message || e.message))" },
    // if (!confirm('确定删除此商品?')) return
    { re: /if \(!confirm\('确定删除此商品\?'\)\) return/g,
      newStr: "if (!await $confirm.confirm('确定删除此商品?')) return" },
    // alert('删除失败')
    { re: /alert\('删除失败'\)/g,
      newStr: "$confirm.error('删除失败')" },
  ],

  // ── StudentHomeView.vue ─────────────────────────────────────────────────
  'views/StudentHomeView.vue': [
    // alert('更换宠物需要先购买宠物更换卡！')
    { re: /alert\('更换宠物需要先购买宠物更换卡！'\)/g,
      newStr: "$confirm.alert('更换宠物需要先购买宠物更换卡！', {type:'warn'})" },
    // if (!confirm('更换宠物需要消耗一张宠物更换卡...')) return
    { re: /if \(myInfo\.petId && !confirm\('更换宠物需要消耗一张宠物更换卡，确定继续吗？'\)\) return/g,
      newStr: "if (myInfo.petId && !await $confirm.confirm('更换宠物需要消耗一张宠物更换卡，确定继续吗？')) return" },
    // alert('领养成功！🐾')
    { re: /alert\('领养成功！🐾'\)/g,
      newStr: "$confirm.success('领养成功！🐾')" },
    // alert(e.response?.data?.error || '领养失败')
    { re: /alert\(e\.response\?\.data\?\.error \|\| '领养失败'\)/g,
      newStr: "$confirm.error(e.response?.data?.error || '领养失败')" },
    // if (!confirm(`确定花费 ${item.price} 分兑换...`)) return
    { re: /if \(!confirm\(`确定花费 \$\{item\.price\} 分兑换「\$\{item\.name\}」吗？`\)\) return/g,
      newStr: "if (!await $confirm.confirm(`确定花费 ${item.price} 分兑换「${item.name}」吗？`)) return" },
    // alert('兑换成功！')
    { re: /alert\('兑换成功！'\)/g,
      newStr: "$confirm.success('兑换成功！')" },
    // alert(e.response?.data?.error || '兑换失败')
    { re: /alert\(e\.response\?\.data\?\.error \|\| '兑换失败'\)/g,
      newStr: "$confirm.error(e.response?.data?.error || '兑换失败')" },
  ],
}

// 需要添加 $confirm import 的文件
const importMap = {
  'components/PetSelectModal.vue': "import $confirm from '../composables/useConfirmModal.js'\n",
  'components/StudentEditModal.vue': "import $confirm from '../composables/useConfirmModal.js'\n",
  'views/ClassroomView.vue': "import $confirm from '../composables/useConfirmModal.js'\n",
  'views/HistoryView.vue': "import $confirm from '../composables/useConfirmModal.js'\n",
  'views/SettingsView.vue': "import $confirm from '../composables/useConfirmModal.js'\n",
  'views/ShopView.vue': "import $confirm from '../composables/useConfirmModal.js'\n",
  'views/StudentHomeView.vue': "import $confirm from '../composables/useConfirmModal.js'\n",
}

// __dirname = frontend/src/, go up one level to reach frontend/
const srcDir = path.join(__dirname, '..', 'src')

for (const [file, rules] of Object.entries(replacements)) {
  const filePath = path.join(srcDir, file)
  if (!fs.existsSync(filePath)) {
    console.warn(`⚠️  文件不存在: ${file}`)
    continue
  }

  let content = fs.readFileSync(filePath, 'utf8')
  const original = content

  for (const { re, newStr } of rules) {
    content = content.replace(re, newStr)
  }

  // 添加 import（如果还没有 $confirm import）
  const importLine = importMap[file]
  if (importLine && !content.includes("import $confirm from '../composables/useConfirmModal.js'")) {
    // 插到 <script setup> 后面的第一个 import 之后
    // 通用策略：插到最后一个 import ... 之后
    const lastImportMatch = content.match(/^import .+ from .+$/mg)
    if (lastImportMatch) {
      const lastImport = lastImportMatch[lastImportMatch.length - 1]
      const lastImportIndex = content.lastIndexOf(lastImport)
      const afterImport = lastImportIndex + lastImport.length
      content = content.slice(0, afterImport) + '\n' + importLine + content.slice(afterImport)
    } else {
      // 没有 import，插到 <script setup> 后面
      content = content.replace(
        /(<script setup>\n?)/,
        '$1\n' + importLine
      )
    }
  }

  if (content !== original) {
    fs.writeFileSync(filePath, content, 'utf8')
    console.log(`✅  已修改: ${file}`)
  } else {
    console.log(`⬜  无变化: ${file}`)
  }
}

console.log('\n完成！请重新构建验证。')
