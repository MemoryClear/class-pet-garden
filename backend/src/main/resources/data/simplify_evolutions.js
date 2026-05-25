const fs = require('fs');
const d = JSON.parse(fs.readFileSync('./evolution_rules.json', 'utf8'));

let changed = 0;

Object.entries(d).forEach(([pokedexId, rules]) => {
  rules.forEach(rule => {
    const c = rule.condition || '';
    const orig = c;

    // 1) Mega / 极巨化 -> 标记未实现
    if (c.indexOf('超级进化') >= 0 || c.indexOf('极巨化') >= 0) {
      rule.implemented = false;
      return;
    }

    // 2) 纯等级进化 -> 不变
    if (/^等级[0-9]/.test(c) && c.indexOf('或') < 0 && c.indexOf('学会') < 0 &&
        c.indexOf('究极') < 0 && c.indexOf('附近') < 0 && c.indexOf('后') < 0 && c.indexOf('对战中') < 0) {
      return;
    }

    // 3) 纯道具进化（仅使用X之石，无其他条件）-> 不变
    if ((/^使用.+之石/.test(c) || /^使用.+之石（.+）$/.test(c)) && c.indexOf('或') < 0) {
      return;
    }

    // 4) 亲密度 / 友好度 / 学会招式提升等级 -> 使用亲密度进化石
    if (c.indexOf('亲密度') >= 0 || c.indexOf('友好度') >= 0 ||
        (c.indexOf('学会') >= 0 && c.indexOf('提升等级') >= 0)) {
      rule.condition = '使用亲密度进化石';
      changed++;
      return;
    }

    // 5) 连接交换进化 -> 使用联系绳（或对应道具）
    if (c.indexOf('连接交换') >= 0) {
      if (c.indexOf('王者之证') >= 0) {
        rule.condition = '使用王者之证';
      } else if (c.indexOf('金属膜') >= 0) {
        rule.condition = '使用金属膜';
      } else if (c.indexOf('升级数据') >= 0) {
        rule.condition = '使用升级数据';
      } else if (c.indexOf('可疑补丁') >= 0) {
        rule.condition = '使用可疑补丁';
      } else if (c.indexOf('护具') >= 0) {
        rule.condition = '使用护具';
      } else if (c.indexOf('龙之鳞片') >= 0) {
        rule.condition = '使用龙之鳞片';
      } else {
        rule.condition = '使用联系绳';
      }
      changed++;
      return;
    }

    // 6) 单独的联系绳条件
    if (c.indexOf('联系绳') >= 0 && c.indexOf('连接交换') < 0) {
      rule.condition = '使用联系绳';
      changed++;
      return;
    }

    // 7) 王者之证单独条件（无需连接交换）
    if (c.indexOf('王者之证') >= 0 && c.indexOf('连接交换') < 0) {
      rule.condition = '使用王者之证';
      changed++;
      return;
    }

    // 8) 混合条件（含"或使用"或"附近"）-> 提取道具名
    if (c.indexOf('或使用') >= 0 || c.indexOf('附近') >= 0) {
      // 提取第一个"使用X"的部分
      const match = c.match(/使用([^，,；;]+)[，,；;]?/);
      if (match) {
        rule.condition = match[0].replace(/[，,；;]$/, '');
      } else {
        // 降级：使用亲密度进化石
        rule.condition = '使用亲密度进化石';
      }
      changed++;
      return;
    }

    // 9) 特殊条件 -> 统一改为亲密度进化石
    if (c.indexOf('后提升等级') >= 0 || c.indexOf('对战中') >= 0 || c.indexOf('究极空间') >= 0 ||
        c.indexOf('其他地区') >= 0 || c.indexOf('学会') >= 0) {
      rule.condition = '使用亲密度进化石';
      changed++;
      return;
    }
  });
});

console.log('Changed rules:', changed);

// 验证伊布
const eevee = d['133'];
console.log('\nEevee (133) rules after simplification:');
if (eevee) eevee.forEach(r => console.log(' ', r.toName, '|', r.condition, '| impl:', r.implemented));

// 统计
let byCond = {};
Object.values(d).flat().forEach(r => {
  const c = r.condition || '(empty)';
  if (!byCond[c]) byCond[c] = 0;
  byCond[c]++;
});
console.log('\nCondition distribution:');
Object.entries(byCond).forEach(([c, n]) => console.log(' ', n, 'x', c));

fs.writeFileSync('./evolution_rules.json', JSON.stringify(d, null, 2), 'utf8');
console.log('\nDone! Written to evolution_rules.json');
